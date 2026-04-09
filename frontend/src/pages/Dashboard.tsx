import { useNavigate } from 'react-router-dom';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import {
  FileText,
  Briefcase,
  MessageSquare,
  ClipboardList,
  ArrowRight,
  Sparkles,
  TrendingUp,
  CheckCircle,
} from 'lucide-react';

export default function Dashboard() {
  const navigate = useNavigate();

  const quickActions = [
    {
      title: '我的简历',
      description: '管理您的简历，支持多版本',
      icon: FileText,
      path: '/resumes',
      color: 'bg-blue-100 text-blue-600',
    },
    {
      title: '职位推荐',
      description: '基于简历智能匹配职位',
      icon: Briefcase,
      path: '/jobs',
      color: 'bg-green-100 text-green-600',
    },
    {
      title: 'AI对话',
      description: '智能助手帮您优化简历',
      icon: MessageSquare,
      path: '/chat',
      color: 'bg-purple-100 text-purple-600',
    },
    {
      title: '求职跟踪',
      description: '追踪求职进度和面试安排',
      icon: ClipboardList,
      path: '/tracking',
      color: 'bg-orange-100 text-orange-600',
    },
  ];

  const stats = [
    { label: '简历数量', value: '3', icon: FileText },
    { label: '匹配职位', value: '12', icon: Briefcase },
    { label: '投递记录', value: '5', icon: ClipboardList },
    { label: 'AI对话', value: '8', icon: MessageSquare },
  ];

  return (
    <div className="space-y-8">
      {/* 欢迎区域 */}
      <div className="bg-gradient-to-r from-blue-600 to-indigo-600 rounded-2xl p-8 text-white">
        <div className="flex flex-col lg:flex-row lg:items-center lg:justify-between gap-6">
          <div>
            <h1 className="text-3xl font-bold mb-2">欢迎回来！</h1>
            <p className="text-blue-100 text-lg">
              智能求职助手已为您准备好今日的求职建议
            </p>
          </div>
          <div className="flex space-x-4">
            <Button
              variant="secondary"
              onClick={() => navigate('/resumes/upload')}
              className="bg-white text-blue-600 hover:bg-blue-50"
            >
              <FileText className="w-4 h-4 mr-2" />
              上传简历
            </Button>
            <Button
              variant="outline"
              onClick={() => navigate('/jobs')}
              className="border-white text-white hover:bg-white/10"
            >
              <Briefcase className="w-4 h-4 mr-2" />
              查看职位
            </Button>
          </div>
        </div>
      </div>

      {/* 统计卡片 */}
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
        {stats.map((stat, index) => {
          const Icon = stat.icon;
          return (
            <Card key={index} className="hover:shadow-md transition-shadow">
              <CardContent className="p-6">
                <div className="flex items-center justify-between">
                  <div>
                    <p className="text-sm text-gray-500">{stat.label}</p>
                    <p className="text-3xl font-bold text-gray-900 mt-1">{stat.value}</p>
                  </div>
                  <div className="w-12 h-12 bg-blue-50 rounded-xl flex items-center justify-center">
                    <Icon className="w-6 h-6 text-blue-600" />
                  </div>
                </div>
              </CardContent>
            </Card>
          );
        })}
      </div>

      {/* 快速操作 */}
      <div>
        <h2 className="text-xl font-bold text-gray-900 mb-4">快速操作</h2>
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
          {quickActions.map((action, index) => {
            const Icon = action.icon;
            return (
              <Card
                key={index}
                className="cursor-pointer hover:shadow-lg transition-all group"
                onClick={() => navigate(action.path)}
              >
                <CardContent className="p-6">
                  <div className={`w-12 h-12 rounded-xl flex items-center justify-center mb-4 ${action.color}`}>
                    <Icon className="w-6 h-6" />
                  </div>
                  <h3 className="font-semibold text-gray-900 mb-1">{action.title}</h3>
                  <p className="text-sm text-gray-500 mb-4">{action.description}</p>
                  <div className="flex items-center text-sm text-blue-600 group-hover:translate-x-1 transition-transform">
                    进入
                    <ArrowRight className="w-4 h-4 ml-1" />
                  </div>
                </CardContent>
              </Card>
            );
          })}
        </div>
      </div>

      {/* 推荐内容 */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* 最新职位推荐 */}
        <Card>
          <CardHeader className="pb-4">
            <div className="flex items-center justify-between">
              <CardTitle className="text-lg flex items-center">
                <Sparkles className="w-5 h-5 mr-2 text-yellow-500" />
                为您推荐
              </CardTitle>
              <Button variant="ghost" size="sm" onClick={() => navigate('/jobs')}>
                查看更多
              </Button>
            </div>
          </CardHeader>
          <CardContent>
            <div className="space-y-4">
              {[
                { title: '高级前端工程师', company: '字节跳动', match: 92 },
                { title: 'Java后端开发', company: '阿里巴巴', match: 85 },
                { title: '产品经理', company: '腾讯', match: 78 },
              ].map((job, index) => (
                <div
                  key={index}
                  className="flex items-center justify-between p-3 bg-gray-50 rounded-lg hover:bg-gray-100 transition-colors cursor-pointer"
                  onClick={() => navigate('/jobs')}
                >
                  <div>
                    <p className="font-medium text-gray-900">{job.title}</p>
                    <p className="text-sm text-gray-500">{job.company}</p>
                  </div>
                  <div className="flex items-center">
                    <span className="text-sm text-green-600 font-medium">{job.match}% 匹配</span>
                  </div>
                </div>
              ))}
            </div>
          </CardContent>
        </Card>

        {/* 求职进展 */}
        <Card>
          <CardHeader className="pb-4">
            <div className="flex items-center justify-between">
              <CardTitle className="text-lg flex items-center">
                <TrendingUp className="w-5 h-5 mr-2 text-green-500" />
                求职进展
              </CardTitle>
              <Button variant="ghost" size="sm" onClick={() => navigate('/tracking')}>
                查看全部
              </Button>
            </div>
          </CardHeader>
          <CardContent>
            <div className="space-y-4">
              {[
                { title: '高级前端工程师', company: '字节跳动', status: '面试中' },
                { title: 'Java后端开发', company: '阿里巴巴', status: '已投递' },
                { title: '产品经理', company: '腾讯', status: '已录用' },
              ].map((app, index) => (
                <div
                  key={index}
                  className="flex items-center justify-between p-3 bg-gray-50 rounded-lg"
                >
                  <div>
                    <p className="font-medium text-gray-900">{app.title}</p>
                    <p className="text-sm text-gray-500">{app.company}</p>
                  </div>
                  <div className="flex items-center">
                    <CheckCircle className="w-4 h-4 mr-1 text-green-500" />
                    <span className="text-sm text-gray-600">{app.status}</span>
                  </div>
                </div>
              ))}
            </div>
          </CardContent>
        </Card>
      </div>
    </div>
  );
}
