import { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { resumeService } from '@/services/resumeService';
import type { ResumeVersion, ResumeGroup } from '@/types';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader } from '@/components/ui/card';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';
import { Badge } from '@/components/ui/badge';
import { Skeleton } from '@/components/ui/skeleton';
import { Textarea } from '@/components/ui/textarea';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog';
import {
  ArrowLeft,
  Save,
  Download,
  Sparkles,
  CheckCircle,
  Loader2,
  AlertCircle,
} from 'lucide-react';
import { toast } from 'sonner';

const versionTypeLabels: Record<string, { label: string; color: string }> = {
  ORIGINAL: { label: '原版', color: 'bg-gray-100 text-gray-700' },
  CONVERTED: { label: '转换版', color: 'bg-blue-100 text-blue-700' },
  AI_OPTIMIZED: { label: 'AI版', color: 'bg-purple-100 text-purple-700' },
};

const statusLabels: Record<string, { label: string; color: string }> = {
  PENDING: { label: '待处理', color: 'bg-yellow-100 text-yellow-700' },
  PROCESSING: { label: '处理中', color: 'bg-blue-100 text-blue-700' },
  COMPLETED: { label: '已完成', color: 'bg-green-100 text-green-700' },
  FAILED: { label: '失败', color: 'bg-red-100 text-red-700' },
};

export default function ResumeEdit() {
  const { groupId } = useParams<{ groupId: string }>();
  const navigate = useNavigate();
  const [resumeGroup, setResumeGroup] = useState<ResumeGroup | null>(null);
  const [versions, setVersions] = useState<ResumeVersion[]>([]);
  const [activeVersion, setActiveVersion] = useState<ResumeVersion | null>(null);
  const [editedContent, setEditedContent] = useState('');
  const [isLoading, setIsLoading] = useState(true);
  const [isSaving, setIsSaving] = useState(false);
  const [hasChanges, setHasChanges] = useState(false);
  const [showLeaveDialog, setShowLeaveDialog] = useState(false);
  const [pendingNavigation, setPendingNavigation] = useState<string | null>(null);

  // 加载简历数据
  useEffect(() => {
    if (!groupId) return;
    loadResumeData();
  }, [groupId]);

  const loadResumeData = async () => {
    try {
      setIsLoading(true);
      const [groupData, versionsData] = await Promise.all([
        resumeService.getResumeGroup(groupId!),
        resumeService.getVersionsByGroup(groupId!),
      ]);
      setResumeGroup(groupData);
      setVersions(versionsData);

      // 默认选中转换版（如果可用）
      const convertedVersion = versionsData.find((v) => v.versionType === 'CONVERTED');
      const firstEditableVersion = versionsData.find((v) => v.editable && v.content);
      const defaultVersion = convertedVersion || firstEditableVersion || versionsData[0];

      if (defaultVersion) {
        setActiveVersion(defaultVersion);
        setEditedContent(defaultVersion.content || '');
      }
    } catch (error) {
      toast.error('加载简历数据失败');
    } finally {
      setIsLoading(false);
    }
  };

  // 处理版本切换
  const handleVersionChange = (versionId: string) => {
    if (hasChanges) {
      setPendingNavigation(versionId);
      setShowLeaveDialog(true);
      return;
    }
    switchToVersion(versionId);
  };

  const switchToVersion = (versionId: string) => {
    const version = versions.find((v) => v.versionId === versionId);
    if (version) {
      setActiveVersion(version);
      setEditedContent(version.content || '');
      setHasChanges(false);
    }
    setPendingNavigation(null);
    setShowLeaveDialog(false);
  };

  // 处理保存
  const handleSave = async () => {
    if (!activeVersion || !activeVersion.editable) return;

    setIsSaving(true);
    try {
      await resumeService.editVersion(activeVersion.versionId, editedContent);
      toast.success('保存成功');
      setHasChanges(false);
      // 刷新版本数据
      loadResumeData();
    } catch (error) {
      toast.error('保存失败');
    } finally {
      setIsSaving(false);
    }
  };

  // 处理下载
  const handleDownload = async () => {
    if (!activeVersion) return;
    try {
      const blob = await resumeService.downloadResume(activeVersion.versionId);
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = `${resumeGroup?.title || 'resume'}.pdf`;
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      window.URL.revokeObjectURL(url);
      toast.success('下载成功');
    } catch (error) {
      toast.error('下载失败');
    }
  };

  // 处理AI优化
  const handleAIOptimize = () => {
    toast.info('AI优化功能即将上线');
  };

  // 处理返回
  const handleBack = () => {
    if (hasChanges) {
      setPendingNavigation('/resumes');
      setShowLeaveDialog(true);
    } else {
      navigate('/resumes');
    }
  };

  // 渲染加载骨架屏
  if (isLoading) {
    return (
      <div className="space-y-6">
        <div className="flex items-center space-x-4">
          <Skeleton className="h-10 w-10" />
          <Skeleton className="h-8 w-48" />
        </div>
        <Skeleton className="h-[600px]" />
      </div>
    );
  }

  if (!resumeGroup || !activeVersion) {
    return (
      <div className="text-center py-16">
        <AlertCircle className="w-16 h-16 text-gray-400 mx-auto mb-4" />
        <h2 className="text-xl font-medium text-gray-900 mb-2">简历不存在</h2>
        <p className="text-gray-500 mb-6">无法找到该简历信息</p>
        <Button onClick={() => navigate('/resumes')}>返回简历列表</Button>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* 顶部导航 */}
      <div className="flex flex-col lg:flex-row lg:items-center lg:justify-between gap-4">
        <div className="flex items-center space-x-4">
          <Button variant="ghost" size="icon" onClick={handleBack}>
            <ArrowLeft className="w-5 h-5" />
          </Button>
          <div>
            <h1 className="text-2xl font-bold text-gray-900">{resumeGroup.title}</h1>
            <p className="text-sm text-gray-500">
              最后更新: {new Date(resumeGroup.updatedAt).toLocaleDateString('zh-CN')}
            </p>
          </div>
        </div>
        <div className="flex items-center space-x-2">
          {activeVersion.editable && (
            <Button
              onClick={handleSave}
              disabled={!hasChanges || isSaving}
              variant={hasChanges ? 'default' : 'outline'}
            >
              {isSaving ? (
                <Loader2 className="w-4 h-4 mr-2 animate-spin" />
              ) : (
                <Save className="w-4 h-4 mr-2" />
              )}
              保存
            </Button>
          )}
          <Button variant="outline" onClick={handleDownload}>
            <Download className="w-4 h-4 mr-2" />
            下载
          </Button>
          <Button variant="outline" onClick={handleAIOptimize}>
            <Sparkles className="w-4 h-4 mr-2" />
            AI优化
          </Button>
        </div>
      </div>

      {/* 版本切换标签 */}
      <Tabs
        value={activeVersion.versionId}
        onValueChange={handleVersionChange}
        className="w-full"
      >
        <TabsList className="grid w-full grid-cols-3 lg:w-auto lg:inline-flex">
          {versions.map((version) => {
            const typeInfo = versionTypeLabels[version.versionType];
            return (
              <TabsTrigger
                key={version.versionId}
                value={version.versionId}
                className="flex items-center space-x-2"
                disabled={!version.content && version.status !== 'COMPLETED'}
              >
                <span>{typeInfo.label}</span>
                {version.status === 'COMPLETED' ? (
                  <CheckCircle className="w-3 h-3 text-green-500" />
                ) : version.status === 'PROCESSING' ? (
                  <Loader2 className="w-3 h-3 animate-spin" />
                ) : null}
              </TabsTrigger>
            );
          })}
        </TabsList>

        {versions.map((version) => (
          <TabsContent key={version.versionId} value={version.versionId} className="mt-6">
            <Card>
              <CardHeader className="pb-4">
                <div className="flex items-center justify-between">
                  <div className="flex items-center space-x-3">
                    <Badge className={versionTypeLabels[version.versionType].color}>
                      {versionTypeLabels[version.versionType].label}
                    </Badge>
                    <Badge className={statusLabels[version.status].color}>
                      {statusLabels[version.status].label}
                    </Badge>
                    {!version.editable && (
                      <Badge variant="outline">只读</Badge>
                    )}
                  </div>
                  <div className="text-sm text-gray-500">
                    {version.fileSize > 0 && `${(version.fileSize / 1024).toFixed(1)} KB`}
                  </div>
                </div>
              </CardHeader>
              <CardContent>
                {version.editable ? (
                  <Textarea
                    value={editedContent}
                    onChange={(e) => {
                      setEditedContent(e.target.value);
                      setHasChanges(true);
                    }}
                    className="min-h-[500px] font-mono text-sm leading-relaxed"
                    placeholder="简历内容为空"
                  />
                ) : (
                  <div className="bg-gray-50 rounded-lg p-6 min-h-[500px]">
                    <p className="text-gray-500 text-center">
                      原版文件不支持在线编辑，请下载后查看
                    </p>
                  </div>
                )}
              </CardContent>
            </Card>
          </TabsContent>
        ))}
      </Tabs>

      {/* 离开确认对话框 */}
      <Dialog open={showLeaveDialog} onOpenChange={setShowLeaveDialog}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>确认离开</DialogTitle>
            <DialogDescription>
              您有未保存的更改，确定要离开吗？未保存的内容将丢失。
            </DialogDescription>
          </DialogHeader>
          <DialogFooter>
            <Button variant="outline" onClick={() => setShowLeaveDialog(false)}>
              取消
            </Button>
            <Button
              variant="destructive"
              onClick={() => {
                if (pendingNavigation === '/resumes') {
                  navigate('/resumes');
                } else if (pendingNavigation) {
                  switchToVersion(pendingNavigation);
                }
              }}
            >
              离开
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
}
