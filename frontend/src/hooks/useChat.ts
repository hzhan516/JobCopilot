import { useEffect, useState, useRef, useMemo, useCallback } from 'react';
import type { Conversation, Message, ResumeGroup, Job } from '@/types';
import { useTranslation } from 'react-i18next';
import chatService from '@/services/chatService';
import { resumeService } from '@/services/resumeService';
import { jobService } from '@/services/jobService';
import { toast } from 'sonner';

const AI_REPLY_POLL_ATTEMPTS = 20;
const AI_REPLY_POLL_INTERVAL_MS = 1500;

function delay(ms: number) {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

function normalizeMessages(conversation: Conversation): Message[] {
  return (conversation.messages ?? []).map((message) => ({
    ...message,
    conversationId: message.conversationId ?? conversation.conversationId,
  }));
}

export interface UseChatReturn {
  conversations: Conversation[];
  activeConversation: Conversation | null;
  messages: Message[];
  inputMessage: string;
  isLoading: boolean;
  isSending: boolean;
  isWaitingForReply: boolean;
  newDialogOpen: boolean;
  newChatTitle: string;
  resumes: ResumeGroup[];
  selectedResumeVersionId: string;
  jobs: Job[];
  selectedJobId: string;
  isCreating: boolean;
  messagesEndRef: React.RefObject<HTMLDivElement | null>;
  activeResumeName: string | null;
  activeJobName: string | null;

  setInputMessage: (value: string) => void;
  setNewDialogOpen: (open: boolean) => void;
  setNewChatTitle: (title: string) => void;
  setSelectedResumeVersionId: (id: string) => void;
  setSelectedJobId: (id: string) => void;
  handleNewDialogOpenChange: (open: boolean) => void;
  handleSelectConversation: (conversation: Conversation) => Promise<void>;
  handleCreateConversation: () => Promise<void>;
  handleSendMessage: () => Promise<void>;
  handleDeleteConversation: (conversationId: string) => Promise<void>;
}

/**
 * 从 Chat.tsx 完整提取的对话状态与逻辑。
 * PR2 前端将复用此 hook 作为 CopilotChatArea 的数据基座。
 */
export function useChat(): UseChatReturn {
  const { t } = useTranslation();
  const [conversations, setConversations] = useState<Conversation[]>([]);
  const [activeConversation, setActiveConversation] = useState<Conversation | null>(null);
  const [messages, setMessages] = useState<Message[]>([]);
  const [inputMessage, setInputMessage] = useState('');
  const [isLoading, setIsLoading] = useState(true);
  const [isSending, setIsSending] = useState(false);
  const [isWaitingForReply, setIsWaitingForReply] = useState(false);

  const [newDialogOpen, setNewDialogOpen] = useState(false);
  const [newChatTitle, setNewChatTitle] = useState('');
  const [resumes, setResumes] = useState<ResumeGroup[]>([]);
  const [selectedResumeVersionId, setSelectedResumeVersionId] = useState('');
  const [jobs, setJobs] = useState<Job[]>([]);
  const [selectedJobId, setSelectedJobId] = useState('');
  const [isCreating, setIsCreating] = useState(false);

  const messagesEndRef = useRef<HTMLDivElement>(null);

  const handleNewDialogOpenChange = useCallback((open: boolean) => {
    setNewDialogOpen(open);
    if (!open) {
      setNewChatTitle('');
      setSelectedResumeVersionId('');
      setSelectedJobId('');
    }
  }, []);

  const syncConversation = useCallback((conversation: Conversation) => {
    setActiveConversation(conversation);
    setMessages(normalizeMessages(conversation));
    setConversations((prev) => {
      const exists = prev.some((item) => item.conversationId === conversation.conversationId);
      if (!exists) {
        return [conversation, ...prev];
      }
      return prev.map((item) =>
        item.conversationId === conversation.conversationId ? conversation : item
      );
    });
  }, []);

  // 初始加载
  useEffect(() => {
    let ignored = false;

    void (async () => {
      try {
        setIsLoading(true);
        const [convs, resumeData, jobData] = await Promise.all([
          chatService.getConversations(),
          Promise.resolve(resumeService.getResumeGroups()).catch(() => null),
          Promise.resolve(jobService.getJobs()).catch(() => null),
        ]);
        if (ignored) return;

        setConversations(convs);
        if (convs.length > 0) {
          syncConversation(convs[0]);
        } else {
          setActiveConversation(null);
          setMessages([]);
        }

        if (resumeData !== null) setResumes(resumeData);
        if (jobData !== null) setJobs(jobData);
      } catch {
        if (!ignored) toast.error(t('chat.loadError'));
      } finally {
        if (!ignored) setIsLoading(false);
      }
    })();

    return () => {
      ignored = true;
    };
  }, [t, syncConversation]);

  // 消息滚动
  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages]);

  const activeResumeName = useMemo(() => {
    const resumeVersionId = activeConversation?.resumeVersionId;
    if (!resumeVersionId) return null;

    for (const group of resumes) {
      const versions = [group.convertedVersion, group.aiOptimizedVersion, group.originalVersion]
        .filter((version): version is NonNullable<typeof version> => Boolean(version));

      if (versions.some((version) => version.versionId === resumeVersionId)) {
        return group.title || resumeVersionId.slice(0, 8);
      }
    }

    return resumeVersionId.slice(0, 8);
  }, [activeConversation?.resumeVersionId, resumes]);

  const activeJobName = useMemo(() => {
    const jobId = activeConversation?.jobId;
    if (!jobId) return null;

    const job = jobs.find((item) => item.id === jobId);
    if (!job) return jobId.slice(0, 8);

    const company = job.parsedContent?.company?.trim();
    const title = job.parsedContent?.title?.trim();
    const parts = [company, title].filter(Boolean);

    return parts.length > 0 ? parts.join(' - ') : jobId.slice(0, 8);
  }, [activeConversation?.jobId, jobs]);

  const handleSelectConversation = useCallback(
    async (conversation: Conversation) => {
      syncConversation(conversation);
      try {
        const detail = await chatService.getConversation(conversation.conversationId);
        syncConversation(detail);
      } catch {
        toast.error(t('chat.loadError'));
      }
    },
    [syncConversation, t]
  );

  const pollForAiReply = useCallback(
    async (conversationId: string, previousMessageCount: number) => {
      for (let attempt = 0; attempt < AI_REPLY_POLL_ATTEMPTS; attempt += 1) {
        await delay(AI_REPLY_POLL_INTERVAL_MS);
        const updatedConversation = await chatService.getConversation(conversationId);
        const updatedMessages = normalizeMessages(updatedConversation);
        syncConversation(updatedConversation);

        const newMessages = updatedMessages.slice(previousMessageCount);
        if (newMessages.some((message) => message.role === 'ASSISTANT')) {
          return true;
        }
      }

      return false;
    },
    [syncConversation]
  );

  const handleCreateConversation = useCallback(async () => {
    if (!selectedResumeVersionId) {
      toast.error(t('chat.resumeRequired'));
      return;
    }
    if (!selectedJobId) {
      toast.error(t('chat.jobRequired'));
      return;
    }

    setIsCreating(true);
    try {
      let finalTitle = newChatTitle.trim();

      // 标题为空时按 简历名称-公司名称-职位 自动生成
      if (!finalTitle) {
        const resumeGroup = resumes.find((group) =>
          [group.convertedVersion, group.aiOptimizedVersion]
            .filter((v): v is NonNullable<typeof v> => !!v && v.exists)
            .some((v) => v.versionId === selectedResumeVersionId)
        );
        const resumeName = resumeGroup?.title || '';

        const selectedJob = jobs.find((j) => j.id === selectedJobId);
        const companyName = selectedJob?.parsedContent?.company || '';
        const jobTitle = selectedJob?.parsedContent?.title || '';

        const parts = [resumeName, companyName, jobTitle].filter((p) => p.trim());
        finalTitle = parts.join('-') || t('chat.newChatTitle');
      }

      const newConversation = await chatService.createConversation(
        finalTitle,
        selectedResumeVersionId,
        selectedJobId
      );
      syncConversation(newConversation);
      setNewDialogOpen(false);
      toast.success(t('chat.createSuccess'));

      // 新对话包含预设消息并已触发异步 AI 请求，轮询等待回复
      const newMessages = normalizeMessages(newConversation);
      if (!newMessages.some((message) => message.role === 'ASSISTANT')) {
        setIsWaitingForReply(true);
        void (async () => {
          try {
            const hasReply = await pollForAiReply(newConversation.conversationId, newMessages.length);
            if (!hasReply) {
              toast.info(t('chat.aiPending'));
            }
          } catch (error) {
            console.error('Failed to poll AI reply after creation', error);
            toast.info(t('chat.aiPending'));
          } finally {
            setIsWaitingForReply(false);
          }
        })();
      }
    } catch {
      toast.error(t('chat.createFailed'));
    } finally {
      setIsCreating(false);
    }
  }, [
    selectedResumeVersionId,
    selectedJobId,
    newChatTitle,
    resumes,
    jobs,
    syncConversation,
    pollForAiReply,
    t,
  ]);

  const handleSendMessage = useCallback(async () => {
    if (!inputMessage.trim() || !activeConversation) {
      console.warn('Chat send skipped', {
        hasInput: Boolean(inputMessage.trim()),
        hasActiveConversation: Boolean(activeConversation),
      });
      return;
    }

    const content = inputMessage.trim();
    const conversationId = activeConversation.conversationId;
    const tempMessageId = `temp-${Date.now()}`;
    const userMessage: Message = {
      messageId: tempMessageId,
      conversationId,
      role: 'USER',
      content,
      createdAt: new Date().toISOString(),
    };

    setMessages((prev) => [...prev, userMessage]);
    setInputMessage('');
    setIsSending(true);

    try {
      const updatedConversation = await chatService.sendMessage(conversationId, content);
      const savedMessages = normalizeMessages(updatedConversation);
      syncConversation(updatedConversation);

      const lastMessage = savedMessages[savedMessages.length - 1];
      if (lastMessage?.role !== 'ASSISTANT') {
        setIsWaitingForReply(true);
        void (async () => {
          try {
            const hasReply = await pollForAiReply(conversationId, savedMessages.length);
            if (!hasReply) {
              toast.info(t('chat.aiPending'));
            }
          } catch (error) {
            console.error('Failed to poll AI reply', error);
            toast.info(t('chat.aiPending'));
          } finally {
            setIsWaitingForReply(false);
          }
        })();
      }
    } catch (error) {
      console.error('Failed to send chat message', error);
      setMessages((prev) => prev.filter((message) => message.messageId !== tempMessageId));
      toast.error(t('chat.sendFailed'));
    } finally {
      setIsSending(false);
    }
  }, [inputMessage, activeConversation, syncConversation, pollForAiReply, t]);

  const handleDeleteConversation = useCallback(
    async (conversationId: string) => {
      try {
        await chatService.deleteConversation(conversationId);
        setConversations((prev) => prev.filter((c) => c.conversationId !== conversationId));
        if (activeConversation?.conversationId === conversationId) {
          setActiveConversation(null);
          setMessages([]);
        }
        toast.success(t('chat.deleteSuccess'));
      } catch {
        toast.error(t('chat.deleteFailed'));
      }
    },
    [activeConversation, t]
  );

  return {
    conversations,
    activeConversation,
    messages,
    inputMessage,
    isLoading,
    isSending,
    isWaitingForReply,
    newDialogOpen,
    newChatTitle,
    resumes,
    selectedResumeVersionId,
    jobs,
    selectedJobId,
    isCreating,
    messagesEndRef,
    activeResumeName,
    activeJobName,

    setInputMessage,
    setNewDialogOpen,
    setNewChatTitle,
    setSelectedResumeVersionId,
    setSelectedJobId,
    handleNewDialogOpenChange,
    handleSelectConversation,
    handleCreateConversation,
    handleSendMessage,
    handleDeleteConversation,
  };
}
