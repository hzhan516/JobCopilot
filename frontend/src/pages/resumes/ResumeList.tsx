import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { resumeService } from '@/services/resumeService';
import type { ResumeGroup } from '@/types';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Skeleton } from '@/components/ui/skeleton';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog';
import {
  FileText,
  Upload,
  Edit,
  Download,
  Trash2,
  MoreVertical,
  File,
  FileCode,
  Sparkles,
  Clock,
  CheckCircle,
  AlertCircle,
} from 'lucide-react';
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu';
import { toast } from 'sonner';

const versionTypeLabels: Record<string, { label: string; icon: React.ElementType; color: string }> = {
  ORIGINAL: { label: '原版', icon: File, color: 'bg-gray-100 text-gray-700' },
  CONVERTED: { label: '转换版', icon: FileCode, color: 'bg-blue-100 text-blue-700' },
  AI_OPTIMIZED: { label: 'AI版', icon: Sparkles, color: 'bg-purple-100 text-purple-700' },
};

const statusLabels: Record<string, { label: string; icon: React.ElementType; color: string }> = {
  PENDING: { label: '待处理', icon: Clock, color: 'text-yellow-600' },
  PROCESSING: { label: '处理中', icon: Clock, color: 'text-blue-600' },
  COMPLETED: { label: '已完成', icon: CheckCircle, color: 'text-green-600' },
  FAILED: { label: '失败', icon: AlertCircle, color: 'text-red-600' },
};

export default function ResumeList() {
  const navigate = useNavigate();
  const [resumes, setResumes] = useState<ResumeGroup[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [deleteDialogOpen, setDeleteDialogOpen] = useState(false);
  const [selectedResume, setSelectedResume] = useState<ResumeGroup | null>(null);

  // 加载简历列表
  const loadResumes = async () => {
    try {
      setIsLoading(true);
      const data = await resumeService.getResumeGroups();
      setResumes(data);
    } catch (error) {
      toast.error('加载简历列表失败');
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    loadResumes();
  }, []);

  // 处理上传
  const handleUpload = () => {
    navigate('/resumes/upload');
  };

  // 处理编辑
  const handleEdit = (groupId: string) => {
    navigate(`/resumes/${groupId}/edit`);
  };

  // 处理下载
  const handleDownload = async (versionId: string, fileName: string) => {
    try {
      const blob = await resumeService.downloadResume(versionId);
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = fileName;
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      window.URL.revokeObjectURL(url);
      toast.success('下载成功');
    } catch (error) {
      toast.error('下载失败');
    }
  };

  // 处理删除
  const handleDelete = async () => {
    if (!selectedResume) return;
    try {
      await resumeService.deleteResumeGroup(selectedResume.groupId);
      toast.success('删除成功');
      loadResumes();
    } catch (error) {
      toast.error('删除失败');
    } finally {
      setDeleteDialogOpen(false);
      setSelectedResume(null);
    }
  };

  // 渲染版本卡片
  const renderVersionCard = (version: { versionId: string; status: string; exists: boolean } | null, type: string) => {
    if (!version || !version.exists) {
      return (
        <div className="flex items-center space-x-2 px-3 py-2 rounded-lg bg-gray-50 text-gray-400">
          <span className="text-sm">未生成</span>
        </div>
      );
    }

    const typeInfo = versionTypeLabels[type];
    const statusInfo = statusLabels[version.status];
    const TypeIcon = typeInfo.icon;
    const StatusIcon = statusInfo.icon;

    return (
      <div className={`flex items-center space-x-2 px-3 py-2 rounded-lg ${typeInfo.color}`}>
        <TypeIcon className="w-4 h-4" />
        <span className="text-sm font-medium">{typeInfo.label}</span>
        <StatusIcon className={`w-3 h-3 ${statusInfo.color}`} />
      </div>
    );
  };

  // 渲染骨架屏
  if (isLoading) {
    return (
      <div className="space-y-6">
        <div className="flex items-center justify-between">
          <div>
            <h1 className="text-3xl font-bold text-gray-900">我的简历</h1>
            <p className="text-gray-500 mt-1">管理您的简历，支持多版本</p>
          </div>
          <Skeleton className="h-10 w-32" />
        </div>
        <div className="grid gap-4">
          {[1, 2, 3].map((i) => (
            <Skeleton key={i} className="h-40" />
          ))}
        </div>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* 页面标题 */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-3xl font-bold text-gray-900">我的简历</h1>
          <p className="text-gray-500 mt-1">管理您的简历，支持原版、转换版、AI版多版本管理</p>
        </div>
        <Button onClick={handleUpload} className="w-full sm:w-auto">
          <Upload className="w-4 h-4 mr-2" />
          上传简历
        </Button>
      </div>

      {/* 简历列表 */}
      {resumes.length === 0 ? (
        <Card className="border-dashed">
          <CardContent className="flex flex-col items-center justify-center py-16">
            <div className="w-16 h-16 bg-gray-100 rounded-full flex items-center justify-center mb-4">
              <FileText className="w-8 h-8 text-gray-400" />
            </div>
            <h3 className="text-lg font-medium text-gray-900 mb-2">还没有简历</h3>
            <p className="text-gray-500 mb-6 text-center max-w-sm">
              上传您的第一份简历，我们将自动解析并生成多个版本供您管理
            </p>
            <Button onClick={handleUpload}>
              <Upload className="w-4 h-4 mr-2" />
              上传简历
            </Button>
          </CardContent>
        </Card>
      ) : (
        <div className="grid gap-4">
          {resumes.map((resume) => (
            <Card key={resume.groupId} className="hover:shadow-md transition-shadow">
              <CardHeader className="pb-4">
                <div className="flex items-start justify-between">
                  <div className="flex items-center space-x-3">
                    <div className="w-10 h-10 bg-blue-100 rounded-lg flex items-center justify-center">
                      <FileText className="w-5 h-5 text-blue-600" />
                    </div>
                    <div>
                      <CardTitle className="text-lg">{resume.title}</CardTitle>
                      <p className="text-sm text-gray-500">
                        创建于 {new Date(resume.createdAt).toLocaleDateString('zh-CN')}
                      </p>
                    </div>
                  </div>
                  <DropdownMenu>
                    <DropdownMenuTrigger asChild>
                      <Button variant="ghost" size="icon">
                        <MoreVertical className="w-4 h-4" />
                      </Button>
                    </DropdownMenuTrigger>
                    <DropdownMenuContent align="end">
                      <DropdownMenuItem onClick={() => handleEdit(resume.groupId)}>
                        <Edit className="w-4 h-4 mr-2" />
                        编辑
                      </DropdownMenuItem>
                      {resume.originalVersion?.exists && (
                        <DropdownMenuItem
                          onClick={() =>
                            handleDownload(
                              resume.originalVersion!.versionId,
                              `${resume.title}.pdf`
                            )
                          }
                        >
                          <Download className="w-4 h-4 mr-2" />
                          下载原版
                        </DropdownMenuItem>
                      )}
                      <DropdownMenuSeparator />
                      <DropdownMenuItem
                        className="text-red-600"
                        onClick={() => {
                          setSelectedResume(resume);
                          setDeleteDialogOpen(true);
                        }}
                      >
                        <Trash2 className="w-4 h-4 mr-2" />
                        删除
                      </DropdownMenuItem>
                    </DropdownMenuContent>
                  </DropdownMenu>
                </div>
              </CardHeader>
              <CardContent>
                <div className="flex flex-wrap gap-2">
                  {renderVersionCard(resume.originalVersion, 'ORIGINAL')}
                  {renderVersionCard(resume.convertedVersion, 'CONVERTED')}
                  {renderVersionCard(resume.aiOptimizedVersion, 'AI_OPTIMIZED')}
                </div>
              </CardContent>
            </Card>
          ))}
        </div>
      )}

      {/* 删除确认对话框 */}
      <Dialog open={deleteDialogOpen} onOpenChange={setDeleteDialogOpen}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>确认删除</DialogTitle>
            <DialogDescription>
              您确定要删除简历 "{selectedResume?.title}" 吗？此操作不可撤销。
            </DialogDescription>
          </DialogHeader>
          <DialogFooter>
            <Button variant="outline" onClick={() => setDeleteDialogOpen(false)}>
              取消
            </Button>
            <Button variant="destructive" onClick={handleDelete}>
              删除
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
}
