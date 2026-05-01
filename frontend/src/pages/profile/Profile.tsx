import { useTranslation } from 'react-i18next';
import { useAuth } from '@/hooks/useAuth';
import { useNavigate } from 'react-router-dom';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import {
  User,
  Mail,
  Shield,
  LogOut,
  ArrowLeft,
  Lock,
  Link as LinkIcon,
  Sparkles,
} from 'lucide-react';

export default function Profile() {
  const { t } = useTranslation();
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  return (
    <div className="space-y-6 max-w-3xl mx-auto">
      {/* 返回按钮 */}
      <Button variant="ghost" onClick={() => navigate('/')} className="pl-0">
        <ArrowLeft className="w-4 h-4 mr-2" />
        {t('common.back')}
      </Button>

      {/* 页面标题 */}
      <div>
        <h1 className="text-3xl font-bold text-gray-900">{t('profile.title')}</h1>
        <p className="text-gray-500 mt-1">{t('profile.subtitle')}</p>
      </div>

      {/* 用户信息卡片 */}
      <Card>
        <CardHeader className="pb-4">
          <CardTitle className="text-lg flex items-center">
            <User className="w-5 h-5 mr-2 text-muted-foreground" />
            {t('profile.basicInfo')}
          </CardTitle>
        </CardHeader>
        <CardContent className="space-y-4">
          <div className="flex items-center space-x-4">
            <div className="w-16 h-16 bg-blue-100 rounded-full flex items-center justify-center">
              <User className="w-8 h-8 text-blue-600" />
            </div>
            <div>
              <p className="text-sm text-gray-500">{t('profile.userId')}</p>
              <p className="font-mono text-sm">{user?.userId || '-'}</p>
            </div>
          </div>

          <div className="grid gap-4 pt-2">
            <div className="flex items-center justify-between p-3 bg-gray-50 rounded-lg">
              <div className="flex items-center space-x-3">
                <Mail className="w-4 h-4 text-gray-500" />
                <span className="text-sm text-gray-600">{t('profile.email')}</span>
              </div>
              <span className="text-sm font-medium">{user?.email || '-'}</span>
            </div>

            <div className="flex items-center justify-between p-3 bg-gray-50 rounded-lg">
              <div className="flex items-center space-x-3">
                <Shield className="w-4 h-4 text-gray-500" />
                <span className="text-sm text-gray-600">{t('profile.accountType')}</span>
              </div>
              <Badge variant="outline">{t('profile.typeEmail')}</Badge>
            </div>
          </div>
        </CardContent>
      </Card>

      {/* 安全设置 */}
      <Card>
        <CardHeader className="pb-4">
          <CardTitle className="text-lg flex items-center">
            <Lock className="w-5 h-5 mr-2 text-muted-foreground" />
            {t('profile.security')}
          </CardTitle>
        </CardHeader>
        <CardContent className="space-y-3">
          <div className="flex items-center justify-between p-3 border rounded-lg">
            <div>
              <p className="font-medium">{t('profile.changePassword')}</p>
              <p className="text-sm text-gray-500">{t('profile.changePasswordDesc')}</p>
            </div>
            <Button variant="outline" disabled>
              {t('common.edit')}
            </Button>
          </div>

          <div className="flex items-center justify-between p-3 border rounded-lg">
            <div className="flex items-center space-x-3">
              <LinkIcon className="w-5 h-5 text-gray-500" />
              <div>
                <p className="font-medium">{t('profile.bindGoogle')}</p>
                <p className="text-sm text-gray-500">{t('profile.bindGoogleDesc')}</p>
              </div>
            </div>
            <Button variant="outline" disabled>
              {t('profile.bind')}
            </Button>
          </div>
        </CardContent>
      </Card>

      {/* AI 功能 */}
      <Card>
        <CardHeader className="pb-4">
          <CardTitle className="text-lg flex items-center">
            <Sparkles className="w-5 h-5 mr-2 text-muted-foreground" />
            {t('profile.aiFeatures')}
          </CardTitle>
        </CardHeader>
        <CardContent>
          <div className="flex items-center justify-between p-3 border rounded-lg">
            <div>
              <p className="font-medium">{t('profile.resumeOptimization')}</p>
              <p className="text-sm text-gray-500">{t('profile.resumeOptimizationDesc')}</p>
            </div>
            <Button variant="outline" onClick={() => navigate('/resumes')}>
              {t('profile.goToResumes')}
            </Button>
          </div>
        </CardContent>
      </Card>

      {/* 退出登录 */}
      <div className="pt-4">
        <Button variant="destructive" onClick={handleLogout} className="w-full sm:w-auto">
          <LogOut className="w-4 h-4 mr-2" />
          {t('layout.userMenu.logout')}
        </Button>
      </div>
    </div>
  );
}
