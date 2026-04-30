import { useEffect, useState, useRef } from 'react';
import type { Conversation, Message } from '@/types';
import { useTranslation } from 'react-i18next';
import { formatTime } from '@/utils/i18n';
import chatService from '@/services/chatService';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Skeleton } from '@/components/ui/skeleton';
import { ScrollArea } from '@/components/ui/scroll-area';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from '@/components/ui/dialog';
import {
  MessageSquare,
  Plus,
  Send,
  Trash2,
  Bot,
  User,
  Loader2,
  MoreVertical,
  Sparkles,
} from 'lucide-react';
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu';
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

export default function Chat() {
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
  const messagesEndRef = useRef<HTMLDivElement>(null);

  const syncConversation = (conversation: Conversation) => {
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
  };

  // 加载对话列表
  useEffect(() => {
    loadConversations();
  }, []);

  // 滚动到底部
  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages]);

  const loadConversations = async () => {
    try {
      setIsLoading(true);
      const data = await chatService.getConversations();
      setConversations(data);
      if (data.length > 0) {
        syncConversation(data[0]);
      } else {
        setActiveConversation(null);
        setMessages([]);
      }
    } catch {
      toast.error(t('chat.loadError'));
    } finally {
      setIsLoading(false);
    }
  };

  const handleSelectConversation = async (conversation: Conversation) => {
    syncConversation(conversation);
    try {
      const detail = await chatService.getConversation(conversation.conversationId);
      syncConversation(detail);
    } catch {
      toast.error(t('chat.loadError'));
    }
  };

  // 创建新对话
  const handleCreateConversation = async () => {
    if (!newChatTitle.trim()) {
      toast.error(t('chat.createError'));
      return;
    }
    try {
      const newConversation = await chatService.createConversation(newChatTitle);
      syncConversation(newConversation);
      setNewChatTitle('');
      setNewDialogOpen(false);
      toast.success(t('chat.createSuccess'));
    } catch {
      toast.error(t('chat.createFailed'));
    }
  };

  const pollForAiReply = async (conversationId: string, previousMessageCount: number) => {
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
  };

  // 发送消息
  const handleSendMessage = async () => {
    console.log('Chat send clicked', {
      inputMessage,
      activeConversationId: activeConversation?.conversationId,
      isSending,
      isWaitingForReply,
    });

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
      console.log('Chat send request starting', { conversationId, content });
      const updatedConversation = await chatService.sendMessage(conversationId, content);
      console.log('Chat send request succeeded', updatedConversation);
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
  };

  // 删除对话
  const handleDeleteConversation = async (conversationId: string) => {
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
  };

  // 渲染消息
  const renderMessage = (message: Message) => {
    const isUser = message.role === 'USER';
    return (
      <div
        key={message.messageId}
        className={`flex ${isUser ? 'justify-end' : 'justify-start'} mb-4`}
      >
        <div
          className={`flex max-w-[80%] ${isUser ? 'flex-row-reverse' : 'flex-row'} items-start`}
        >
          <div
            className={`w-8 h-8 rounded-full flex items-center justify-center flex-shrink-0 ${
              isUser ? 'bg-blue-600 ml-2' : 'bg-purple-100 mr-2'
            }`}
          >
            {isUser ? (
              <User className="w-4 h-4 text-white" />
            ) : (
              <Bot className="w-4 h-4 text-purple-600" />
            )}
          </div>
          <div
            className={`rounded-2xl px-4 py-3 ${
              isUser
                ? 'bg-blue-600 text-white'
                : 'bg-gray-100 text-gray-900'
            }`}
          >
            <p className="whitespace-pre-wrap">{message.content}</p>
            <span
              className={`text-xs mt-1 block ${
                isUser ? 'text-blue-200' : 'text-gray-500'
              }`}
            >
              {formatTime(message.createdAt)}
            </span>
          </div>
        </div>
      </div>
    );
  };

  // 渲染加载状态
  if (isLoading) {
    return (
      <div className="h-[calc(100vh-8rem)] flex gap-6">
        <div className="w-64 hidden lg:block">
          <Skeleton className="h-full" />
        </div>
        <div className="flex-1">
          <Skeleton className="h-full" />
        </div>
      </div>
    );
  }

  return (
    <div className="h-[calc(100vh-8rem)] flex gap-6">
      {/* 侧边栏 - 对话列表 */}
      <div className="w-64 hidden lg:flex flex-col bg-white rounded-xl border shadow-sm">
        <div className="p-4 border-b">
          <Dialog open={newDialogOpen} onOpenChange={setNewDialogOpen}>
            <DialogTrigger asChild>
              <Button className="w-full">
                <Plus className="w-4 h-4 mr-2" />
                {t('chat.newChat')}
              </Button>
            </DialogTrigger>
            <DialogContent>
              <DialogHeader>
                <DialogTitle>{t('chat.newChatTitle')}</DialogTitle>
                <DialogDescription>{t('chat.newChatDesc')}</DialogDescription>
              </DialogHeader>
              <Input
                placeholder={t('chat.chatTitlePlaceholder')}
                value={newChatTitle}
                onChange={(e) => setNewChatTitle(e.target.value)}
              />
              <DialogFooter>
                <Button variant="outline" onClick={() => setNewDialogOpen(false)}>
                  {t('common.cancel')}
                </Button>
                <Button onClick={handleCreateConversation}>{t('chat.create')}</Button>
              </DialogFooter>
            </DialogContent>
          </Dialog>
        </div>
        <ScrollArea className="flex-1">
          <div className="p-2 space-y-1">
            {conversations.map((conversation) => (
              <div
                key={conversation.conversationId}
                className={`flex items-center justify-between p-3 rounded-lg cursor-pointer transition-colors ${
                  activeConversation?.conversationId === conversation.conversationId
                    ? 'bg-blue-50 text-blue-900'
                    : 'hover:bg-gray-100'
                }`}
                onClick={() => handleSelectConversation(conversation)}
              >
                <div className="flex items-center space-x-3 overflow-hidden">
                  <MessageSquare className="w-4 h-4 flex-shrink-0" />
                  <span className="text-sm font-medium truncate">{conversation.title}</span>
                </div>
                <DropdownMenu>
                  <DropdownMenuTrigger asChild>
                    <Button
                      variant="ghost"
                      size="icon"
                      className="h-6 w-6 opacity-0 group-hover:opacity-100"
                      onClick={(e) => e.stopPropagation()}
                    >
                      <MoreVertical className="w-3 h-3" />
                    </Button>
                  </DropdownMenuTrigger>
                  <DropdownMenuContent align="end">
                    <DropdownMenuItem
                      className="text-red-600"
                      onClick={() => handleDeleteConversation(conversation.conversationId)}
                    >
                      <Trash2 className="w-4 h-4 mr-2" />
                      {t('chat.delete')}
                    </DropdownMenuItem>
                  </DropdownMenuContent>
                </DropdownMenu>
              </div>
            ))}
          </div>
        </ScrollArea>
      </div>

      {/* 主聊天区域 */}
      <div className="flex-1 flex flex-col bg-white rounded-xl border shadow-sm overflow-hidden">
        {activeConversation ? (
          <>
            {/* 聊天头部 */}
            <div className="border-b p-4">
              <div className="flex items-center justify-between">
                <div className="flex items-center space-x-3">
                  <div className="w-8 h-8 bg-purple-100 rounded-full flex items-center justify-center">
                    <Sparkles className="w-4 h-4 text-purple-600" />
                  </div>
                  <div>
                    <h3 className="font-semibold text-gray-900">{activeConversation.title}</h3>
                    <p className="text-xs text-gray-500">{t('chat.aiAssistant')}</p>
                  </div>
                </div>
              </div>
            </div>

            {/* 消息列表 */}
            <ScrollArea className="flex-1 p-4">
              {messages.length === 0 ? (
                <div className="h-full flex flex-col items-center justify-center text-gray-500">
                  <Bot className="w-16 h-16 mb-4 text-gray-300" />
                  <p>{t('chat.emptyState')}</p>
                </div>
              ) : (
                <>
                  {messages.map(renderMessage)}
                  {isWaitingForReply && (
                    <div className="flex justify-start mb-4">
                      <div className="flex items-center space-x-2 bg-gray-100 rounded-2xl px-4 py-3">
                        <Loader2 className="w-4 h-4 animate-spin" />
                        <span className="text-sm text-gray-600">{t('chat.aiThinking')}</span>
                      </div>
                    </div>
                  )}
                  <div ref={messagesEndRef} />
                </>
              )}
            </ScrollArea>

            {/* 输入框 */}
            <div className="border-t p-4">
              <div className="flex space-x-2">
                <Input
                  placeholder={t('chat.inputPlaceholder')}
                  value={inputMessage}
                  onChange={(e) => setInputMessage(e.target.value)}
                  onKeyDown={(e) => {
                    if (e.key === 'Enter' && !e.shiftKey) {
                      e.preventDefault();
                      handleSendMessage();
                    }
                  }}
                  disabled={isSending}
                  className="flex-1"
                />
                <Button
                  onClick={handleSendMessage}
                  disabled={!inputMessage.trim() || isSending}
                >
                  {isSending ? (
                    <Loader2 className="w-4 h-4 animate-spin" />
                  ) : (
                    <Send className="w-4 h-4" />
                  )}
                </Button>
              </div>
              <p className="text-xs text-gray-500 mt-2 text-center">{t('chat.footerHint')}</p>
            </div>
          </>
        ) : (
          <div className="flex-1 flex flex-col items-center justify-center text-gray-500">
            <MessageSquare className="w-16 h-16 mb-4 text-gray-300" />
            <p className="mb-4">{t('chat.selectOrCreate')}</p>
            <Button onClick={() => setNewDialogOpen(true)}>
              <Plus className="w-4 h-4 mr-2" />
              {t('chat.newChat')}
            </Button>
          </div>
        )}
      </div>
    </div>
  );
}
