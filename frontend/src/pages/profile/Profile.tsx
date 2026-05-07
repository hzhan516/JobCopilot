import { useEffect, useMemo } from 'react';
import { useTranslation } from 'react-i18next';
import { useNavigate } from 'react-router-dom';
import { z } from 'zod';
import { useForm, useWatch } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { useAuth } from '@/hooks/useAuth';
import { useProfileStore } from '@/store/profile.store';
import { useTimeZone } from '@/hooks/useTimeZone';
import { getUserTimeZone } from '@/utils/i18n';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import {
  Form,
  FormControl,
  FormField,
  FormItem,
  FormLabel,
  FormMessage,
} from '@/components/ui/form';
import {
  User,
  Mail,
  Shield,
  LogOut,
  ArrowLeft,
  Lock,
  Link as LinkIcon,
  Sparkles,
  Phone,
  Briefcase,
  MapPin,
  Save,
  Loader2,
  Globe,
} from 'lucide-react';
import { Badge } from '@/components/ui/badge';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select';
import { toast } from 'sonner';

/**
 * Returns the browser's supported time zones or a curated fallback list.
 * Fallback is used when Intl.supportedValuesOf is unavailable (older browsers).
 *
 * 获取常用时区列表；在旧浏览器中回退到精选列表
 */
function getCommonTimeZones(): string[] {
  try {
    if (typeof Intl !== 'undefined' && 'supportedValuesOf' in Intl) {
      return (Intl as unknown as { supportedValuesOf: (key: string) => string[] }).supportedValuesOf('timeZone');
    }
  } catch {
    // fallback
  }
  return [
    'UTC',
    'America/New_York',
    'America/Chicago',
    'America/Denver',
    'America/Los_Angeles',
    'America/Toronto',
    'America/Sao_Paulo',
    'Europe/London',
    'Europe/Paris',
    'Europe/Berlin',
    'Europe/Moscow',
    'Asia/Dubai',
    'Asia/Kolkata',
    'Asia/Shanghai',
    'Asia/Hong_Kong',
    'Asia/Tokyo',
    'Asia/Seoul',
    'Asia/Singapore',
    'Asia/Bangkok',
    'Australia/Sydney',
    'Australia/Melbourne',
    'Pacific/Auckland',
    'Pacific/Honolulu',
  ];
}

function useProfileSchema() {
  const { t } = useTranslation();
  return useMemo(
    () =>
      z.object({
        fullName: z.string().max(255, t('profile.validation.maxLength')),
        phone: z.string().max(50, t('profile.validation.maxLength')),
        targetPosition: z.string().max(255, t('profile.validation.maxLength')),
        preferredLocation: z.string().max(255, t('profile.validation.maxLength')),
        avatarUrl: z.string().max(1000, t('profile.validation.maxLength')),
      }),
    [t]
  );
}

type ProfileFormValues = z.infer<ReturnType<typeof useProfileSchema>>;

export default function Profile() {
  const { t } = useTranslation();
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const { profile, loading, fetchProfile, updateProfile, updateAvatar } = useProfileStore();
  const { timeZone, updateTimeZone, resetTimeZone } = useTimeZone();

  const profileSchema = useProfileSchema();

  const form = useForm<ProfileFormValues>({
    resolver: zodResolver(profileSchema),
    defaultValues: {
      fullName: '',
      phone: '',
      targetPosition: '',
      preferredLocation: '',
      avatarUrl: '',
    },
  });

  useEffect(() => {
    fetchProfile();
  }, [fetchProfile]);

  // Sync form values when profile loads to avoid stale empty defaults
  // 表单值同步：profile 加载完成后回填，避免空默认值残留
  useEffect(() => {
    if (profile) {
      form.reset({
        fullName: profile.fullName || '',
        phone: profile.phone || '',
        targetPosition: profile.targetPosition || '',
        preferredLocation: profile.preferredLocation || '',
        avatarUrl: profile.avatarUrl || '',
      });
    }
  }, [profile, form]);

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  const onSubmit = async (values: ProfileFormValues) => {
    try {
      await updateProfile({
        fullName: values.fullName,
        phone: values.phone,
        targetPosition: values.targetPosition,
        preferredLocation: values.preferredLocation,
      });
      await updateAvatar({ avatarUrl: values.avatarUrl });
      toast.success(t('profile.saveSuccess'));
    } catch {
      toast.error(t('profile.saveError'));
    }
  };

  const avatarUrl = useWatch({ control: form.control, name: 'avatarUrl' });

  return (
    <div className="space-y-6 max-w-3xl mx-auto">
      <Button variant="ghost" onClick={() => navigate('/')} className="pl-0">
        <ArrowLeft className="w-4 h-4 mr-2" />
        {t('common.back')}
      </Button>

      <div>
        <h1 className="text-3xl font-bold text-gray-900">{t('profile.title')}</h1>
        <p className="text-gray-500 mt-1">{t('profile.subtitle')}</p>
      </div>

      <Card>
        <CardHeader className="pb-4">
          <CardTitle className="text-lg flex items-center">
            <User className="w-5 h-5 mr-2 text-muted-foreground" />
            {t('profile.basicInfo')}
          </CardTitle>
        </CardHeader>
        <CardContent className="space-y-4">
          <div className="flex items-center space-x-4">
            <div className="w-16 h-16 bg-blue-100 rounded-full flex items-center justify-center overflow-hidden">
              {avatarUrl ? (
                <img src={avatarUrl} alt="avatar" className="w-full h-full object-cover" />
              ) : (
                <User className="w-8 h-8 text-blue-600" />
              )}
            </div>
            <div>
              <p className="text-sm text-gray-500">{t('profile.userId')}</p>
              <p className="font-mono text-sm">{user?.userId || '-'}</p>
            </div>
          </div>

          <Form {...form}>
            <form onSubmit={form.handleSubmit(onSubmit)} className="grid gap-4 pt-2">
              <div className="flex items-center justify-between p-3 bg-gray-50 rounded-lg">
                <div className="flex items-center space-x-3">
                  <Mail className="w-4 h-4 text-gray-500" />
                  <span className="text-sm text-gray-600">{t('profile.email')}</span>
                </div>
                <span className="text-sm font-medium">{user?.email || '-'}</span>
              </div>

              <FormField
                control={form.control}
                name="fullName"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel className="flex items-center space-x-2">
                      <User className="w-4 h-4 text-gray-500" />
                      <span>{t('profile.fullName')}</span>
                    </FormLabel>
                    <FormControl>
                      <Input placeholder={t('profile.fullNamePlaceholder')} {...field} />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />

              <FormField
                control={form.control}
                name="phone"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel className="flex items-center space-x-2">
                      <Phone className="w-4 h-4 text-gray-500" />
                      <span>{t('profile.phone')}</span>
                    </FormLabel>
                    <FormControl>
                      <Input placeholder={t('profile.phonePlaceholder')} {...field} />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />

              <FormField
                control={form.control}
                name="targetPosition"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel className="flex items-center space-x-2">
                      <Briefcase className="w-4 h-4 text-gray-500" />
                      <span>{t('profile.targetPosition')}</span>
                    </FormLabel>
                    <FormControl>
                      <Input placeholder={t('profile.targetPositionPlaceholder')} {...field} />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />

              <FormField
                control={form.control}
                name="preferredLocation"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel className="flex items-center space-x-2">
                      <MapPin className="w-4 h-4 text-gray-500" />
                      <span>{t('profile.preferredLocation')}</span>
                    </FormLabel>
                    <FormControl>
                      <Input placeholder={t('profile.preferredLocationPlaceholder')} {...field} />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />

              <FormField
                control={form.control}
                name="avatarUrl"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel className="flex items-center space-x-2">
                      <User className="w-4 h-4 text-gray-500" />
                      <span>{t('profile.avatar')}</span>
                    </FormLabel>
                    <FormControl>
                      <Input placeholder={t('profile.avatarPlaceholder')} {...field} />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />

              <div className="flex items-center justify-between p-3 bg-gray-50 rounded-lg">
                <div className="flex items-center space-x-3">
                  <Shield className="w-4 h-4 text-gray-500" />
                  <span className="text-sm text-gray-600">{t('profile.accountType')}</span>
                </div>
                <Badge variant="outline">{t('profile.typeEmail')}</Badge>
              </div>

              <div className="pt-2">
                <Button type="submit" disabled={loading} className="w-full sm:w-auto">
                  {loading ? (
                    <Loader2 className="w-4 h-4 mr-2 animate-spin" />
                  ) : (
                    <Save className="w-4 h-4 mr-2" />
                  )}
                  {t('common.save')}
                </Button>
              </div>
            </form>
          </Form>
        </CardContent>
      </Card>

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

      <Card>
        <CardHeader className="pb-4">
          <CardTitle className="text-lg flex items-center">
            <Globe className="w-5 h-5 mr-2 text-muted-foreground" />
            {t('profile.preferences')}
          </CardTitle>
        </CardHeader>
        <CardContent className="space-y-3">
          <div className="flex items-center justify-between p-3 border rounded-lg">
            <div>
              <p className="font-medium">{t('profile.timeZone')}</p>
              <p className="text-sm text-gray-500">
                {t('profile.timeZoneAuto', { timeZone: getUserTimeZone() })}
              </p>
            </div>
            <Select
              value={timeZone}
              onValueChange={(tz) => {
                if (tz === 'auto') {
                  resetTimeZone();
                } else {
                  updateTimeZone(tz);
                }
              }}
            >
              <SelectTrigger className="w-[240px]">
                <SelectValue placeholder={t('profile.timeZone')} />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="auto">
                  {t('profile.timeZoneAuto', { timeZone: Intl.DateTimeFormat().resolvedOptions().timeZone })}
                </SelectItem>
                {getCommonTimeZones().map((tz) => (
                  <SelectItem key={tz} value={tz}>
                    {tz}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>
        </CardContent>
      </Card>

      <div className="pt-4">
        <Button variant="destructive" onClick={handleLogout} className="w-full sm:w-auto">
          <LogOut className="w-4 h-4 mr-2" />
          {t('layout.userMenu.logout')}
        </Button>
      </div>
    </div>
  );
}
