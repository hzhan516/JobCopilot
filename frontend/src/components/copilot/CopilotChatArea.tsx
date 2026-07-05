import { useEffect } from 'react';
import type { Message } from '@/types';
import { useTranslation } from 'react-i18next';
import { formatTime } from '@/utils/i18n';
import { useChat } from '@/hooks/useChat';
import { useCopilotStore } from '@/store/copilot.store';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select';
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
  FileText,
  Briefcase,
} from 'lucide-react';
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu';

/**
 * Copilot Drawer 内部的紧凑对话 UI。
 * 复用 useChat hook，与 Chat.tsx 共享全部对话逻辑。
 * 当从 Job 详情页唤起时，自动填入 Job context（由 copilot.store 驱动）。
 */
export default function CopilotChatArea() {
  const { t } = useTranslation();
  const {
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
  } = useChat();
  const { context } = useCopilotStore();

  // 从 Job 详情页唤起时自动选中对应 Job
  useEffect(() => {
    if (context?.type === 'job' && context.id) {
      setSelectedJobId(context.id);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [context?.type === 'job' ? (context as { type: 'job'; id: string }).id : null]);

  const renderMessage = (message: Message) => {
    const isUser = message.role === 'USER';
    return (
      <div
        key={message.messageId}
        className={`flex ${isUser ? 'justify-end' : 'justify-start'} mb-3`}
      >
        <div
          className={`flex max-w-[90%] ${isUser ? 'flex-row-reverse' : 'flex-row'} items-start`}
        >
          <div
            className={`w-7 h-7 rounded-full flex items-center justify-center flex-shrink-0 ${
              isUser ? 'bg-blue-600 ml-2' : 'bg-purple-100 mr-2'
            }`}
          >
            {isUser ? (
              <User className="w-3.5 h-3.5 text-white" />
            ) : (
              <Bot className="w-3.5 h-3.5 text-purple-600" />
            )}
          </div>
          <div
            className={`rounded-2xl px-3 py-2 text-sm ${
              isUser ? 'bg-blue-600 text-white' : 'bg-gray-100 text-gray-900'
            }`}
          >
            <p className="whitespace-pre-wrap">{message.content}</p>
            <span
              className={`text-[10px] mt-1 block ${
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

  // 加载态
  if (isLoading) {
    return (
      <div className="flex-1 flex items-center justify-center">
        <Loader2 className="w-5 h-5 animate-spin text-muted-foreground" />
      </div>
    );
  }

  return (
    <div className="flex flex-col h-full">
      {/* 顶部：对话选择 + 新建 */}
      <div className="border-b p-3 flex items-center justify-between shrink-0">
        <div className="flex-1 min-w-0 mr-2">
          <select
            className="w-full text-sm font-medium bg-transparent border rounded px-2 py-1.5 truncate"
            value={activeConversation?.conversationId ?? ''}
            onChange={(e) => {
              const conv = conversations.find((c) => c.conversationId === e.target.value);
              if (conv) handleSelectConversation(conv);
            }}
          >
            {conversations.length === 0 && (
              <option value="">{t('chat.selectOrCreate')}</option>
            )}
            {conversations.map((c) => (
              <option key={c.conversationId} value={c.conversationId}>
                {c.title}
              </option>
            ))}
          </select>
        </div>

        <Dialog open={newDialogOpen} onOpenChange={handleNewDialogOpenChange}>
          <DialogTrigger asChild>
            <Button size="sm" variant="outline">
              <Plus className="w-4 h-4" />
            </Button>
          </DialogTrigger>
          <DialogContent className="sm:max-w-md">
            <DialogHeader>
              <DialogTitle>{t('chat.newChatTitle')}</DialogTitle>
              <DialogDescription>{t('chat.newChatDesc')}</DialogDescription>
            </DialogHeader>
            <div className="space-y-4 py-2">
              <div>
                <label className="text-sm font-medium mb-2 block">
                  {t('chat.chatTitlePlaceholder')}
                  <span className="text-gray-400 ml-1 text-xs">({t('common.optional')})</span>
                </label>
                <Input
                  placeholder={t('chat.chatTitlePlaceholder')}
                  value={newChatTitle}
                  onChange={(e) => setNewChatTitle(e.target.value)}
                />
                <p className="text-xs text-gray-500 mt-1">{t('chat.titleAutoHint')}</p>
              </div>

              <div>
                <label className="text-sm font-medium mb-2 block">
                  {t('chat.selectResume')}
                  <span className="text-red-500 ml-1">*</span>
                </label>
                <Select
                  value={selectedResumeVersionId}
                  onValueChange={setSelectedResumeVersionId}
                >
                  <SelectTrigger>
                    <SelectValue placeholder={t('chat.selectResumePlaceholder')} />
                  </SelectTrigger>
                  <SelectContent>
                    {resumes.map((group) =>
                      [group.convertedVersion, group.aiOptimizedVersion]
                        .filter((v): v is NonNullable<typeof v> => !!v && v.exists)
                        .map((version) => {
                          const label = `${group.title} - ${version.versionId.slice(0, 8)} (${version.status})`;
                          return (
                            <SelectItem key={version.versionId} value={version.versionId}>
                              {label}
                            </SelectItem>
                          );
                        })
                    )}
                  </SelectContent>
                </Select>
              </div>

              <div>
                <label className="text-sm font-medium mb-2 block">
                  {t('chat.selectJob')}
                  <span className="text-red-500 ml-1">*</span>
                </label>
                <Select value={selectedJobId} onValueChange={setSelectedJobId}>
                  <SelectTrigger>
                    <SelectValue placeholder={t('chat.selectJobPlaceholder')} />
                  </SelectTrigger>
                  <SelectContent>
                    {jobs
                      .filter((job) => job.status === 'COMPLETED')
                      .map((job) => (
                        <SelectItem key={job.id} value={job.id}>
                          {job.parsedContent?.company || t('jobDetail.unknownCompany')}
                          {' - '}
                          {job.parsedContent?.title || t('jobDetail.unknownTitle')}
                        </SelectItem>
                      ))}
                  </SelectContent>
                </Select>
                {jobs.filter((job) => job.status === 'COMPLETED').length === 0 && (
                  <p className="text-xs text-orange-500 mt-1">{t('chat.noAvailableJobs')}</p>
                )}
              </div>
            </div>
            <DialogFooter>
              <Button
                variant="outline"
                onClick={() => setNewDialogOpen(false)}
                disabled={isCreating}
              >
                {t('common.cancel')}
              </Button>
              <Button
                onClick={handleCreateConversation}
                disabled={
                  isCreating || !selectedResumeVersionId || !selectedJobId
                }
              >
                {isCreating ? (
                  <Loader2 className="w-4 h-4 mr-2 animate-spin" />
                ) : (
                  <Sparkles className="w-4 h-4 mr-2" />
                )}
                {t('chat.create')}
              </Button>
            </DialogFooter>
          </DialogContent>
        </Dialog>

        {/* 删除当前对话 */}
        {activeConversation && (
          <DropdownMenu>
            <DropdownMenuTrigger asChild>
              <Button variant="ghost" size="icon" className="h-7 w-7 ml-1">
                <MoreVertical className="w-3.5 h-3.5" />
              </Button>
            </DropdownMenuTrigger>
            <DropdownMenuContent align="end">
              <DropdownMenuItem
                className="text-red-600"
                onClick={() =>
                  handleDeleteConversation(activeConversation!.conversationId)
                }
              >
                <Trash2 className="w-4 h-4 mr-2" />
                {t('chat.delete')}
              </DropdownMenuItem>
            </DropdownMenuContent>
          </DropdownMenu>
        )}
      </div>

      {/* 对话上下文标签 */}
      {(activeResumeName || activeJobName) && (
        <div className="flex items-center gap-2 px-3 py-1.5 border-b bg-muted/30 shrink-0">
          {activeResumeName && (
            <span
              className="inline-flex items-center text-[10px] px-1.5 py-0.5 rounded bg-blue-50 text-blue-600 dark:bg-blue-900/30 dark:text-blue-400"
              title={activeResumeName}
            >
              <FileText className="w-3 h-3 mr-0.5" />
              <span className="truncate max-w-[120px]">
                {t('chat.selectedResumeLabel', { name: activeResumeName })}
              </span>
            </span>
          )}
          {activeJobName && (
            <span
              className="inline-flex items-center text-[10px] px-1.5 py-0.5 rounded bg-green-50 text-green-600 dark:bg-green-900/30 dark:text-green-400"
              title={activeJobName}
            >
              <Briefcase className="w-3 h-3 mr-0.5" />
              <span className="truncate max-w-[120px]">
                {t('chat.selectedJobLabel', { name: activeJobName })}
              </span>
            </span>
          )}
        </div>
      )}

      {/* 消息区域 */}
      <div className="flex-1 overflow-y-auto p-3">
        {!activeConversation ? (
          <div className="h-full flex flex-col items-center justify-center text-muted-foreground">
            <MessageSquare className="w-10 h-10 mb-3 opacity-30" />
            <p className="text-sm">{t('chat.selectOrCreate')}</p>
          </div>
        ) : messages.length === 0 ? (
          <div className="h-full flex flex-col items-center justify-center text-muted-foreground">
            <Bot className="w-10 h-10 mb-3 opacity-30" />
            <p className="text-sm">{t('chat.emptyState')}</p>
          </div>
        ) : (
          <>
            {messages.map(renderMessage)}
            {isWaitingForReply && (
              <div className="flex justify-start mb-3">
                <div className="flex items-center space-x-2 bg-gray-100 rounded-2xl px-3 py-2">
                  <Loader2 className="w-3.5 h-3.5 animate-spin" />
                  <span className="text-xs text-gray-600">{t('chat.aiThinking')}</span>
                </div>
              </div>
            )}
            <div ref={messagesEndRef} />
          </>
        )}
      </div>

      {/* 输入区域 */}
      {activeConversation && (
        <div className="border-t p-3 shrink-0">
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
              className="flex-1 text-sm"
            />
            <Button
              size="sm"
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
        </div>
      )}
    </div>
  );
}
