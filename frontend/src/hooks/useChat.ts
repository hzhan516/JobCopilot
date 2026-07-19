import { useEffect, useState, useRef, useMemo, useCallback } from 'react';
import type { Conversation, Message, ResumeGroup, Job } from '@/types';
import { useTranslation } from 'react-i18next';
import chatService from '@/services/chatService';
import { resumeService } from '@/services/resumeService';
import { jobService } from '@/services/jobService';
import { toast } from 'sonner';

const AI_REPLY_POLL_INTERVALS_MS = [1000, 2000, 3000, 5000] as const;

function delay(ms: number, signal: AbortSignal) {
  return new Promise<void>((resolve, reject) => {
    const timeoutId = window.setTimeout(resolve, ms);
    signal.addEventListener('abort', () => {
      window.clearTimeout(timeoutId);
      reject(new DOMException('Polling aborted', 'AbortError'));
    }, { once: true });
  });
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
  retryAiReply: () => Promise<void>;
  handleDeleteConversation: (conversationId: string) => Promise<void>;
  compactConversation: () => Promise<void>;
  isCompacting: boolean;
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
  const [isCompacting, setIsCompacting] = useState(false);

  const messagesEndRef = useRef<HTMLDivElement>(null);
  const pollAbortRef = useRef<AbortController | null>(null);
  const pollingRequestIdRef = useRef<string | null>(null);

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
    setIsWaitingForReply(conversation.aiReply?.status === 'PENDING');
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
      pollAbortRef.current?.abort();
      pollingRequestIdRef.current = null;
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
    async (conversationId: string, requestId: string, signal: AbortSignal) => {
      let attempt = 0;
      while (!signal.aborted) {
        const interval = AI_REPLY_POLL_INTERVALS_MS[
          Math.min(attempt, AI_REPLY_POLL_INTERVALS_MS.length - 1)
        ];
        await delay(interval, signal);
        let updatedConversation: Conversation;
        try {
          updatedConversation = await chatService.getConversation(conversationId, signal);
        } catch (error) {
          if (signal.aborted) return;
          console.warn('Transient failure while polling AI reply', error);
          attempt += 1;
          continue;
        }
        const reply = updatedConversation.aiReply;
        syncConversation(updatedConversation);

        // Never let an older assistant message or a different request terminate this wait.
        if (!reply || reply.requestId !== requestId || reply.status !== 'PENDING') {
          return;
        }
        attempt += 1;
      }
    },
    [syncConversation]
  );

  const beginPolling = useCallback((conversation: Conversation) => {
    const requestId = conversation.aiReply?.requestId;
    if (!requestId || conversation.aiReply?.status !== 'PENDING') return;
    if (pollingRequestIdRef.current === requestId) return;

    pollAbortRef.current?.abort();
    const controller = new AbortController();
    pollAbortRef.current = controller;
    pollingRequestIdRef.current = requestId;
    setIsWaitingForReply(true);

    void pollForAiReply(conversation.conversationId, requestId, controller.signal)
      .catch((error: unknown) => {
        if (!(error instanceof DOMException && error.name === 'AbortError')) {
          console.error('Failed to poll AI reply', error);
          toast.info(t('chat.aiPending'));
        }
      })
      .finally(() => {
        if (pollingRequestIdRef.current === requestId) {
          pollingRequestIdRef.current = null;
          pollAbortRef.current = null;
        }
      });
  }, [pollForAiReply, t]);

  useEffect(() => () => pollAbortRef.current?.abort(), []);

  useEffect(() => {
    if (activeConversation?.aiReply?.status === 'PENDING') {
      beginPolling(activeConversation);
    }
  }, [activeConversation?.conversationId, activeConversation?.aiReply?.requestId,
    activeConversation?.aiReply?.status, beginPolling]);

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
      beginPolling(newConversation);
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
    beginPolling,
    t,
  ]);

  const handleSendMessage = useCallback(async () => {
    if (!inputMessage.trim() || !activeConversation || isWaitingForReply) {
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
      syncConversation(updatedConversation);
      beginPolling(updatedConversation);
    } catch (error) {
      console.error('Failed to send chat message', error);
      setMessages((prev) => prev.filter((message) => message.messageId !== tempMessageId));
      toast.error(t('chat.sendFailed'));
    } finally {
      setIsSending(false);
    }
  }, [inputMessage, activeConversation, isWaitingForReply, syncConversation, beginPolling, t]);

  const retryAiReply = useCallback(async () => {
    if (!activeConversation || isWaitingForReply) return;
    setIsSending(true);
    try {
      const updatedConversation = await chatService.retryAiReply(activeConversation.conversationId);
      syncConversation(updatedConversation);
      beginPolling(updatedConversation);
    } catch (error) {
      console.error('Failed to retry AI reply', error);
      toast.error(t('chat.retryFailed'));
    } finally {
      setIsSending(false);
    }
  }, [activeConversation, isWaitingForReply, syncConversation, beginPolling, t]);

  const compactConversation = useCallback(async () => {
    if (!activeConversation) return;
    setIsCompacting(true);
    try {
      const updated = await chatService.compactConversation(activeConversation.conversationId);
      syncConversation(updated);
      toast.success(t('chat.context.compactSuccess'));
    } catch {
      toast.error(t('chat.context.compactError'));
    } finally {
      setIsCompacting(false);
    }
  }, [activeConversation, syncConversation, t]);

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
    retryAiReply,
    handleDeleteConversation,
    compactConversation,
    isCompacting,
  };
}
