import { useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
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
  const { t } = useTranslation();
  const navigate = useNavigate();

  const quickActions = [
    {
      titleKey: 'dashboard.quickActions.resumes',
      descKey: 'dashboard.quickActions.resumesDesc',
      icon: FileText,
      path: '/resumes',
      color: 'bg-blue-100 text-blue-600',
    },
    {
      titleKey: 'dashboard.quickActions.jobs',
      descKey: 'dashboard.quickActions.jobsDesc',
      icon: Briefcase,
      path: '/jobs',
      color: 'bg-green-100 text-green-600',
    },
    {
      titleKey: 'dashboard.quickActions.chat',
      descKey: 'dashboard.quickActions.chatDesc',
      icon: MessageSquare,
      path: '/chat',
      color: 'bg-purple-100 text-purple-600',
    },
    {
      titleKey: 'dashboard.quickActions.tracking',
      descKey: 'dashboard.quickActions.trackingDesc',
      icon: ClipboardList,
      path: '/tracking',
      color: 'bg-orange-100 text-orange-600',
    },
  ];

  const stats = [
    { labelKey: 'dashboard.stats.resumes', value: '3', icon: FileText },
    { labelKey: 'dashboard.stats.matches', value: '12', icon: Briefcase },
    { labelKey: 'dashboard.stats.applications', value: '5', icon: ClipboardList },
    { labelKey: 'dashboard.stats.chats', value: '8', icon: MessageSquare },
  ];

  return (
    <div className="space-y-8">
      {/* 欢迎区域 */}
      <div className="bg-gradient-to-r from-blue-600 to-indigo-600 rounded-2xl p-8 text-white">
        <div className="flex flex-col lg:flex-row lg:items-center lg:justify-between gap-6">
          <div>
            <h1 className="text-3xl font-bold mb-2">{t('dashboard.welcome')}</h1>
            <p className="text-blue-100 text-lg">{t('dashboard.subtitle')}</p>
          </div>
          <div className="flex space-x-4">
            <Button
              variant="secondary"
              onClick={() => navigate('/resumes/upload')}
              className="bg-white text-blue-600 hover:bg-blue-50"
            >
              <FileText className="w-4 h-4 mr-2" />
              {t('dashboard.uploadResume')}
            </Button>
            <Button
              variant="outline"
              onClick={() => navigate('/jobs')}
              className="border-white text-white hover:bg-white/10"
            >
              <Briefcase className="w-4 h-4 mr-2" />
              {t('dashboard.viewJobs')}
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
                    <p className="text-sm text-gray-500">{t(stat.labelKey)}</p>
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
        <h2 className="text-xl font-bold text-gray-900 mb-4">{t('dashboard.quickActions.title')}</h2>
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
                  <h3 className="font-semibold text-gray-900 mb-1">{t(action.titleKey)}</h3>
                  <p className="text-sm text-gray-500 mb-4">{t(action.descKey)}</p>
                  <div className="flex items-center text-sm text-blue-600 group-hover:translate-x-1 transition-transform">
                    {t('common.enter')}
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
                {t('dashboard.recommendedJobs.title')}
              </CardTitle>
              <Button variant="ghost" size="sm" onClick={() => navigate('/jobs')}>
                {t('common.viewMore')}
              </Button>
            </div>
          </CardHeader>
          <CardContent>
            <div className="space-y-4">
              {[
                { title: 'Senior Frontend Engineer', company: 'Google', match: 92 },
                { title: 'Java Backend Developer', company: 'Amazon', match: 85 },
                { title: 'Product Manager', company: 'Microsoft', match: 78 },
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
                    <span className="text-sm text-green-600 font-medium">
                      {job.match}% {t('dashboard.recommendedJobs.matchLabel')}
                    </span>
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
                {t('dashboard.applicationProgress.title')}
              </CardTitle>
              <Button variant="ghost" size="sm" onClick={() => navigate('/tracking')}>
                {t('common.viewAll')}
              </Button>
            </div>
          </CardHeader>
          <CardContent>
            <div className="space-y-4">
              {[
                { title: 'Senior Frontend Engineer', company: 'Google', statusKey: 'dashboard.applicationProgress.status.interview' },
                { title: 'Java Backend Developer', company: 'Amazon', statusKey: 'dashboard.applicationProgress.status.applied' },
                { title: 'Product Manager', company: 'Microsoft', statusKey: 'dashboard.applicationProgress.status.offered' },
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
                    <span className="text-sm text-gray-600">{t(app.statusKey)}</span>
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
