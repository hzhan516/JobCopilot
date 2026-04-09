import { useEffect, useState } from 'react';
import type { JobApplication } from '@/types';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { Skeleton } from '@/components/ui/skeleton';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select';
import {
  ClipboardList,
  Plus,
  Building2,
  Briefcase,
  Calendar,
  MoreHorizontal,
  Trash2,
} from 'lucide-react';
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu';
import { toast } from 'sonner';

// 模拟投递数据
const mockApplications: JobApplication[] = [
  {
    applicationId: '1',
    jobId: '1',
    jobTitle: '高级前端工程师',
    company: '字节跳动',
    status: 'INTERVIEW',
    appliedAt: '2024-01-10T10:00:00',
    notes: '已通过初筛，等待面试安排',
  },
  {
    applicationId: '2',
    jobId: '2',
    jobTitle: 'Java后端开发',
    company: '阿里巴巴',
    status: 'APPLIED',
    appliedAt: '2024-01-12T14:30:00',
    notes: '',
  },
  {
    applicationId: '3',
    jobId: '3',
    jobTitle: '产品经理',
    company: '腾讯',
    status: 'OFFER',
    appliedAt: '2024-01-05T09:00:00',
    notes: '已收到offer，正在考虑',
  },
];

const statusLabels: Record<string, { label: string; color: string }> = {
  APPLIED: { label: '已投递', color: 'bg-blue-100 text-blue-700' },
  SCREENING: { label: '筛选中', color: 'bg-yellow-100 text-yellow-700' },
  INTERVIEW: { label: '面试中', color: 'bg-purple-100 text-purple-700' },
  OFFER: { label: '已录用', color: 'bg-green-100 text-green-700' },
  REJECTED: { label: '已拒绝', color: 'bg-red-100 text-red-700' },
  WITHDRAWN: { label: '已撤回', color: 'bg-gray-100 text-gray-700' },
};

export default function Tracking() {
  const [applications, setApplications] = useState<JobApplication[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [addDialogOpen, setAddDialogOpen] = useState(false);
  const [newApplication, setNewApplication] = useState({
    jobTitle: '',
    company: '',
    status: 'APPLIED' as JobApplication['status'],
    notes: '',
  });

  // 加载投递记录
  useEffect(() => {
    loadApplications();
  }, []);

  const loadApplications = async () => {
    try {
      setIsLoading(true);
      // 使用模拟数据
      // const data = await trackingService.getApplications();
      await new Promise((resolve) => setTimeout(resolve, 500));
      setApplications(mockApplications);
    } catch (error) {
      toast.error('加载投递记录失败');
    } finally {
      setIsLoading(false);
    }
  };

  // 添加投递记录
  const handleAddApplication = async () => {
    if (!newApplication.jobTitle || !newApplication.company) {
      toast.error('请填写完整信息');
      return;
    }
    try {
      const application: JobApplication = {
        applicationId: Date.now().toString(),
        jobId: '',
        jobTitle: newApplication.jobTitle,
        company: newApplication.company,
        status: newApplication.status,
        appliedAt: new Date().toISOString(),
        notes: newApplication.notes,
      };
      setApplications([application, ...applications]);
      setNewApplication({ jobTitle: '', company: '', status: 'APPLIED', notes: '' });
      setAddDialogOpen(false);
      toast.success('添加成功');
    } catch (error) {
      toast.error('添加失败');
    }
  };

  // 更新状态
  const handleUpdateStatus = async (applicationId: string, status: JobApplication['status']) => {
    try {
      setApplications((prev) =>
        prev.map((app) => (app.applicationId === applicationId ? { ...app, status } : app))
      );
      toast.success('状态更新成功');
    } catch (error) {
      toast.error('更新失败');
    }
  };

  // 删除投递记录
  const handleDeleteApplication = async (applicationId: string) => {
    try {
      setApplications((prev) => prev.filter((app) => app.applicationId !== applicationId));
      toast.success('删除成功');
    } catch (error) {
      toast.error('删除失败');
    }
  };

  // 统计各状态数量
  const statusCounts = applications.reduce((acc, app) => {
    acc[app.status] = (acc[app.status] || 0) + 1;
    return acc;
  }, {} as Record<string, number>);

  // 渲染加载状态
  if (isLoading) {
    return (
      <div className="space-y-6">
        <div className="flex items-center justify-between">
          <div>
            <h1 className="text-3xl font-bold text-gray-900">求职跟踪</h1>
            <p className="text-gray-500 mt-1">管理您的求职进度</p>
          </div>
          <Skeleton className="h-10 w-32" />
        </div>
        <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
          {[1, 2, 3, 4].map((i) => (
            <Skeleton key={i} className="h-24" />
          ))}
        </div>
        <Skeleton className="h-[400px]" />
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* 页面标题 */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-3xl font-bold text-gray-900">求职跟踪</h1>
          <p className="text-gray-500 mt-1">管理您的求职进度</p>
        </div>
        <Button onClick={() => setAddDialogOpen(true)}>
          <Plus className="w-4 h-4 mr-2" />
          添加记录
        </Button>
      </div>

      {/* 统计卡片 */}
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
        <Card>
          <CardContent className="p-4">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-sm text-gray-500">已投递</p>
                <p className="text-2xl font-bold text-blue-600">
                  {statusCounts.APPLIED || 0}
                </p>
              </div>
              <div className="w-10 h-10 bg-blue-100 rounded-full flex items-center justify-center">
                <ClipboardList className="w-5 h-5 text-blue-600" />
              </div>
            </div>
          </CardContent>
        </Card>
        <Card>
          <CardContent className="p-4">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-sm text-gray-500">面试中</p>
                <p className="text-2xl font-bold text-purple-600">
                  {statusCounts.INTERVIEW || 0}
                </p>
              </div>
              <div className="w-10 h-10 bg-purple-100 rounded-full flex items-center justify-center">
                <Briefcase className="w-5 h-5 text-purple-600" />
              </div>
            </div>
          </CardContent>
        </Card>
        <Card>
          <CardContent className="p-4">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-sm text-gray-500">已录用</p>
                <p className="text-2xl font-bold text-green-600">
                  {statusCounts.OFFER || 0}
                </p>
              </div>
              <div className="w-10 h-10 bg-green-100 rounded-full flex items-center justify-center">
                <Building2 className="w-5 h-5 text-green-600" />
              </div>
            </div>
          </CardContent>
        </Card>
        <Card>
          <CardContent className="p-4">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-sm text-gray-500">总计</p>
                <p className="text-2xl font-bold text-gray-900">{applications.length}</p>
              </div>
              <div className="w-10 h-10 bg-gray-100 rounded-full flex items-center justify-center">
                <Calendar className="w-5 h-5 text-gray-600" />
              </div>
            </div>
          </CardContent>
        </Card>
      </div>

      {/* 投递记录列表 */}
      <Card>
        <CardHeader className="pb-4">
          <h3 className="text-lg font-semibold">投递记录</h3>
        </CardHeader>
        <CardContent>
          {applications.length === 0 ? (
            <div className="text-center py-12">
              <ClipboardList className="w-16 h-16 text-gray-300 mx-auto mb-4" />
              <h3 className="text-lg font-medium text-gray-900 mb-2">暂无投递记录</h3>
              <p className="text-gray-500 mb-4">添加您的第一笔投递记录</p>
              <Button onClick={() => setAddDialogOpen(true)}>
                <Plus className="w-4 h-4 mr-2" />
                添加记录
              </Button>
            </div>
          ) : (
            <div className="space-y-4">
              {applications.map((application) => (
                <div
                  key={application.applicationId}
                  className="flex items-start justify-between p-4 border rounded-lg hover:bg-gray-50 transition-colors"
                >
                  <div className="flex-1">
                    <div className="flex items-center space-x-3 mb-2">
                      <h4 className="font-semibold text-gray-900">{application.jobTitle}</h4>
                      <Badge className={statusLabels[application.status].color}>
                        {statusLabels[application.status].label}
                      </Badge>
                    </div>
                    <div className="flex items-center space-x-4 text-sm text-gray-500 mb-2">
                      <span className="flex items-center">
                        <Building2 className="w-4 h-4 mr-1" />
                        {application.company}
                      </span>
                      <span className="flex items-center">
                        <Calendar className="w-4 h-4 mr-1" />
                        {new Date(application.appliedAt).toLocaleDateString('zh-CN')}
                      </span>
                    </div>
                    {application.notes && (
                      <p className="text-sm text-gray-600">{application.notes}</p>
                    )}
                  </div>
                  <div className="flex items-center space-x-2">
                    <Select
                      value={application.status}
                      onValueChange={(value) =>
                        handleUpdateStatus(application.applicationId, value as JobApplication['status'])
                      }
                    >
                      <SelectTrigger className="w-32">
                        <SelectValue />
                      </SelectTrigger>
                      <SelectContent>
                        {Object.entries(statusLabels).map(([key, { label }]) => (
                          <SelectItem key={key} value={key}>
                            {label}
                          </SelectItem>
                        ))}
                      </SelectContent>
                    </Select>
                    <DropdownMenu>
                      <DropdownMenuTrigger asChild>
                        <Button variant="ghost" size="icon">
                          <MoreHorizontal className="w-4 h-4" />
                        </Button>
                      </DropdownMenuTrigger>
                      <DropdownMenuContent align="end">
                        <DropdownMenuItem
                          onClick={() => handleDeleteApplication(application.applicationId)}
                          className="text-red-600"
                        >
                          <Trash2 className="w-4 h-4 mr-2" />
                          删除
                        </DropdownMenuItem>
                      </DropdownMenuContent>
                    </DropdownMenu>
                  </div>
                </div>
              ))}
            </div>
          )}
        </CardContent>
      </Card>

      {/* 添加记录对话框 */}
      <Dialog open={addDialogOpen} onOpenChange={setAddDialogOpen}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>添加投递记录</DialogTitle>
            <DialogDescription>记录您的求职投递信息</DialogDescription>
          </DialogHeader>
          <div className="space-y-4">
            <div className="space-y-2">
              <Label>职位名称</Label>
              <Input
                placeholder="例如：前端工程师"
                value={newApplication.jobTitle}
                onChange={(e) =>
                  setNewApplication({ ...newApplication, jobTitle: e.target.value })
                }
              />
            </div>
            <div className="space-y-2">
              <Label>公司名称</Label>
              <Input
                placeholder="例如：阿里巴巴"
                value={newApplication.company}
                onChange={(e) =>
                  setNewApplication({ ...newApplication, company: e.target.value })
                }
              />
            </div>
            <div className="space-y-2">
              <Label>当前状态</Label>
              <Select
                value={newApplication.status}
                onValueChange={(value) =>
                  setNewApplication({
                    ...newApplication,
                    status: value as JobApplication['status'],
                  })
                }
              >
                <SelectTrigger>
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  {Object.entries(statusLabels).map(([key, { label }]) => (
                    <SelectItem key={key} value={key}>
                      {label}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
            <div className="space-y-2">
              <Label>备注</Label>
              <Input
                placeholder="可选"
                value={newApplication.notes}
                onChange={(e) =>
                  setNewApplication({ ...newApplication, notes: e.target.value })
                }
              />
            </div>
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={() => setAddDialogOpen(false)}>
              取消
            </Button>
            <Button onClick={handleAddApplication}>添加</Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
}
