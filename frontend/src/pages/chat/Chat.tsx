import { useEffect, useState, useRef } from 'react';
import type { Conversation, Message } from '@/types';
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

// 模拟消息数据
const mockMessages: Message[] = [
  {
    messageId: '1',
    conversationId: '1',
    role: 'ASSISTANT',
    content: '您好！我是您的AI求职助手。我可以帮您优化简历、推荐职位、解答求职相关问题。请问有什么可以帮助您的？',
    createdAt: '2024-01-15T10:00:00',
  },
];

export default function Chat() {
  const [conversations, setConversations] = useState<Conversation[]>([]);
  const [activeConversation, setActiveConversation] = useState<Conversation | null>(null);
  const [messages, setMessages] = useState<Message[]>([]);
  const [inputMessage, setInputMessage] = useState('');
  const [isLoading, setIsLoading] = useState(true);
  const [isSending, setIsSending] = useState(false);
  const [newDialogOpen, setNewDialogOpen] = useState(false);
  const [newChatTitle, setNewChatTitle] = useState('');
  const messagesEndRef = useRef<HTMLDivElement>(null);

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
      // 使用模拟数据
      // const data = await chatService.getConversations();
      await new Promise((resolve) => setTimeout(resolve, 500));
      const mockConversations: Conversation[] = [
        {
          conversationId: '1',
          title: '简历优化咨询',
          createdAt: '2024-01-15T10:00:00',
          updatedAt: '2024-01-15T10:30:00',
        },
      ];
      setConversations(mockConversations);
      if (mockConversations.length > 0) {
        setActiveConversation(mockConversations[0]);
        setMessages(mockMessages);
      }
    } catch (error) {
      toast.error('加载对话列表失败');
    } finally {
      setIsLoading(false);
    }
  };

  // 创建新对话
  const handleCreateConversation = async () => {
    if (!newChatTitle.trim()) {
      toast.error('请输入对话标题');
      return;
    }
    try {
      // const newConversation = await chatService.createConversation(newChatTitle);
      const newConversation: Conversation = {
        conversationId: Date.now().toString(),
        title: newChatTitle,
        createdAt: new Date().toISOString(),
        updatedAt: new Date().toISOString(),
      };
      setConversations([newConversation, ...conversations]);
      setActiveConversation(newConversation);
      setMessages([]);
      setNewChatTitle('');
      setNewDialogOpen(false);
      toast.success('创建成功');
    } catch (error) {
      toast.error('创建对话失败');
    }
  };

  // 发送消息
  const handleSendMessage = async () => {
    if (!inputMessage.trim() || !activeConversation) return;

    const userMessage: Message = {
      messageId: Date.now().toString(),
      conversationId: activeConversation.conversationId,
      role: 'USER',
      content: inputMessage,
      createdAt: new Date().toISOString(),
    };

    setMessages((prev) => [...prev, userMessage]);
    setInputMessage('');
    setIsSending(true);

    try {
      // 模拟AI回复
      await new Promise((resolve) => setTimeout(resolve, 1500));
      const aiMessage: Message = {
        messageId: (Date.now() + 1).toString(),
        conversationId: activeConversation.conversationId,
        role: 'ASSISTANT',
        content: `感谢您的提问！关于"${userMessage.content}"，我建议您：\n\n1. 首先完善您的简历内容，突出相关技能和经验\n2. 针对目标岗位定制简历关键词\n3. 准备相关的项目案例和工作成果\n\n如果您需要更详细的建议，请告诉我您的具体情况。`,
        createdAt: new Date().toISOString(),
      };
      setMessages((prev) => [...prev, aiMessage]);
    } catch (error) {
      toast.error('发送失败');
    } finally {
      setIsSending(false);
    }
  };

  // 删除对话
  const handleDeleteConversation = async (conversationId: string) => {
    try {
      // await chatService.deleteConversation(conversationId);
      setConversations((prev) => prev.filter((c) => c.conversationId !== conversationId));
      if (activeConversation?.conversationId === conversationId) {
        setActiveConversation(null);
        setMessages([]);
      }
      toast.success('删除成功');
    } catch (error) {
      toast.error('删除失败');
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
              {new Date(message.createdAt).toLocaleTimeString('zh-CN', {
                hour: '2-digit',
                minute: '2-digit',
              })}
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
                新建对话
              </Button>
            </DialogTrigger>
            <DialogContent>
              <DialogHeader>
                <DialogTitle>新建对话</DialogTitle>
                <DialogDescription>输入对话标题开始新的咨询</DialogDescription>
              </DialogHeader>
              <Input
                placeholder="对话标题"
                value={newChatTitle}
                onChange={(e) => setNewChatTitle(e.target.value)}
              />
              <DialogFooter>
                <Button variant="outline" onClick={() => setNewDialogOpen(false)}>
                  取消
                </Button>
                <Button onClick={handleCreateConversation}>创建</Button>
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
                onClick={() => setActiveConversation(conversation)}
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
                      删除
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
                    <p className="text-xs text-gray-500">AI求职助手</p>
                  </div>
                </div>
              </div>
            </div>

            {/* 消息列表 */}
            <ScrollArea className="flex-1 p-4">
              {messages.length === 0 ? (
                <div className="h-full flex flex-col items-center justify-center text-gray-500">
                  <Bot className="w-16 h-16 mb-4 text-gray-300" />
                  <p>开始新的对话吧</p>
                </div>
              ) : (
                <>
                  {messages.map(renderMessage)}
                  {isSending && (
                    <div className="flex justify-start mb-4">
                      <div className="flex items-center space-x-2 bg-gray-100 rounded-2xl px-4 py-3">
                        <Loader2 className="w-4 h-4 animate-spin" />
                        <span className="text-sm text-gray-600">AI正在思考...</span>
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
                  placeholder="输入您的问题..."
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
              <p className="text-xs text-gray-500 mt-2 text-center">
                AI助手可以帮您优化简历、推荐职位、解答求职问题
              </p>
            </div>
          </>
        ) : (
          <div className="flex-1 flex flex-col items-center justify-center text-gray-500">
            <MessageSquare className="w-16 h-16 mb-4 text-gray-300" />
            <p className="mb-4">选择一个对话或创建新对话</p>
            <Button onClick={() => setNewDialogOpen(true)}>
              <Plus className="w-4 h-4 mr-2" />
              新建对话
            </Button>
          </div>
        )}
      </div>
    </div>
  );
}
