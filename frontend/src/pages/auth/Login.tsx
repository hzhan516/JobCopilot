import { useState, useMemo } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { GoogleLogin } from '@react-oauth/google';
import { useAuth } from '@/hooks/useAuth';
import { z } from 'zod';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Checkbox } from '@/components/ui/checkbox';
import { Card, CardContent, CardDescription, CardFooter, CardHeader, CardTitle } from '@/components/ui/card';
import { Alert, AlertDescription } from '@/components/ui/alert';
import { Form, FormControl, FormField, FormItem, FormLabel, FormMessage } from '@/components/ui/form';
import { Dialog, DialogContent, DialogDescription, DialogHeader, DialogTitle } from '@/components/ui/dialog';
import SliderCaptcha from '@/components/SliderCaptcha';
import { FileText, Loader2, Eye, EyeOff } from 'lucide-react';
import { LanguageSwitcher } from '@/components/LanguageSwitcher';

function useLoginSchema() {
  const { t } = useTranslation();
  return useMemo(
    () =>
      z.object({
        email: z.string().min(1, t('auth.login.errors.emailRequired')).email(t('auth.login.errors.emailInvalid')),
        password: z
          .string()
          .min(1, t('auth.login.errors.passwordRequired'))
          .min(6, t('auth.login.errors.passwordMin'))
          .max(32, t('auth.login.errors.passwordMax')),
        rememberMe: z.boolean(),
      }),
    [t]
  );
}

type LoginFormValues = z.infer<ReturnType<typeof useLoginSchema>>;

export default function Login() {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const { login, loginByGoogle } = useAuth();
  const [showPassword, setShowPassword] = useState(false);
  const [error, setError] = useState('');
  const [googleLoading, setGoogleLoading] = useState(false);

  // 人机验证状态 / CAPTCHA state
  const [captchaToken, setCaptchaToken] = useState('');
  const [captchaKey, setCaptchaKey] = useState(0);
  const [showGoogleCaptchaModal, setShowGoogleCaptchaModal] = useState(false);
  const [googleCaptchaVerified, setGoogleCaptchaVerified] = useState(false);

  const loginSchema = useLoginSchema();

  const form = useForm<LoginFormValues>({
    resolver: zodResolver(loginSchema),
    defaultValues: {
      email: '',
      password: '',
      rememberMe: false,
    },
  });

  const { isSubmitting } = form.formState;

  const resetCaptcha = () => {
    setCaptchaToken('');
    setCaptchaKey((prev) => prev + 1);
  };

  const onSubmit = async (values: LoginFormValues) => {
    setError('');

    if (!captchaToken) {
      setError(t('auth.captcha.required'));
      return;
    }

    try {
      await login({ email: values.email, password: values.password, captchaToken }, values.rememberMe);
      navigate('/resumes', { replace: true });
    } catch {
      setError(t('auth.login.errorInvalidCredentials'));
      // 任何业务错误都重置人机验证
      // Reset CAPTCHA on any business error
      resetCaptcha();
    }
  };

  const handleGoogleSuccess = async (credentialResponse: { credential?: string }) => {
    if (!credentialResponse.credential) {
      setError(t('auth.login.googleLoginFailed'));
      return;
    }
    setError('');
    setGoogleLoading(true);
    try {
      await loginByGoogle({ idToken: credentialResponse.credential, captchaToken }, false);
      navigate('/resumes', { replace: true });
    } catch {
      setError(t('auth.login.googleLoginFailed'));
      resetCaptcha();
    } finally {
      setGoogleLoading(false);
    }
  };

  const handleGoogleCaptchaVerified = (token: string) => {
    setCaptchaToken(token);
    setGoogleCaptchaVerified(true);
    setShowGoogleCaptchaModal(false);
  };

  return (
    <div className="min-h-screen flex items-center justify-center bg-gradient-to-br from-blue-50 to-indigo-100 px-4 relative">
      <div className="absolute top-4 right-4">
        <LanguageSwitcher />
      </div>
      <div className="w-full max-w-md">
        <div className="flex justify-center mb-8">
          <div className="flex items-center space-x-3">
            <div className="w-12 h-12 bg-blue-600 rounded-xl flex items-center justify-center shadow-lg">
              <FileText className="w-7 h-7 text-white" />
            </div>
            <span className="text-2xl font-bold text-gray-900">{t('common.appName')}</span>
          </div>
        </div>

        <Card className="shadow-xl border-0">
          <CardHeader className="space-y-1">
            <CardTitle className="text-2xl font-bold text-center">{t('auth.login.title')}</CardTitle>
            <CardDescription className="text-center">{t('auth.login.subtitle')}</CardDescription>
          </CardHeader>

          <CardContent>
            {error && (
              <Alert variant="destructive" className="mb-4">
                <AlertDescription>{error}</AlertDescription>
              </Alert>
            )}

            <Form {...form}>
              <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-4">
                <FormField
                  control={form.control}
                  name="email"
                  render={({ field }) => (
                    <FormItem>
                      <FormLabel>{t('auth.login.emailLabel')}</FormLabel>
                      <FormControl>
                        <Input
                          type="email"
                          placeholder={t('auth.login.emailPlaceholder')}
                          disabled={isSubmitting}
                          className="h-11"
                          {...field}
                        />
                      </FormControl>
                      <FormMessage />
                    </FormItem>
                  )}
                />

                <FormField
                  control={form.control}
                  name="password"
                  render={({ field }) => (
                    <FormItem>
                      <FormLabel>{t('auth.login.passwordLabel')}</FormLabel>
                      <FormControl>
                        <div className="relative">
                          <Input
                            type={showPassword ? 'text' : 'password'}
                            placeholder={t('auth.login.passwordPlaceholder')}
                            disabled={isSubmitting}
                            className="h-11 pr-10"
                            {...field}
                          />
                          <button
                            type="button"
                            onClick={() => setShowPassword(!showPassword)}
                            className="absolute right-3 top-1/2 -translate-y-1/2 text-gray-500 hover:text-gray-700"
                            tabIndex={-1}
                          >
                            {showPassword ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                          </button>
                        </div>
                      </FormControl>
                      <FormMessage />
                    </FormItem>
                  )}
                />

                <FormField
                  control={form.control}
                  name="rememberMe"
                  render={({ field }) => (
                    <FormItem className="flex flex-row items-start space-x-3 space-y-0 py-1">
                      <FormControl>
                        <Checkbox checked={field.value} onCheckedChange={field.onChange} disabled={isSubmitting} />
                      </FormControl>
                      <div className="space-y-1 leading-none">
                        <FormLabel className="font-normal cursor-pointer">{t('auth.login.rememberMe')}</FormLabel>
                      </div>
                    </FormItem>
                  )}
                />

                {/* 人机验证 / CAPTCHA */}
                <SliderCaptcha
                  key={captchaKey}
                  onVerified={(token) => setCaptchaToken(token)}
                  onError={() => setError(t('auth.captcha.required'))}
                  className="mx-auto"
                />

                <Button type="submit" className="w-full h-11" disabled={isSubmitting || !captchaToken}>
                  {isSubmitting ? (
                    <>
                      <Loader2 className="w-4 h-4 mr-2 animate-spin" />
                      {t('auth.login.loggingIn')}
                    </>
                  ) : (
                    t('auth.login.loginButton')
                  )}
                </Button>
              </form>
            </Form>

            <div className="relative my-4">
              <div className="absolute inset-0 flex items-center">
                <span className="w-full border-t" />
              </div>
              <div className="relative flex justify-center text-xs uppercase">
                <span className="bg-white px-2 text-muted-foreground">{t('auth.login.or')}</span>
              </div>
            </div>

            <div className="flex justify-center">
              {googleLoading ? (
                <Loader2 className="w-6 h-6 animate-spin text-primary" />
              ) : googleCaptchaVerified ? (
                <GoogleLogin
                  onSuccess={handleGoogleSuccess}
                  onError={() => setError(t('auth.login.googleLoginFailed'))}
                  useOneTap
                />
              ) : (
                <Button
                  type="button"
                  variant="outline"
                  className="w-full h-11"
                  onClick={() => setShowGoogleCaptchaModal(true)}
                >
                  <svg className="w-5 h-5 mr-2" viewBox="0 0 24 24">
                    <path
                      fill="#4285F4"
                      d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92a5.06 5.06 0 0 1-2.2 3.32v2.77h3.57c2.08-1.92 3.28-4.74 3.27-8.1z"
                    />
                    <path
                      fill="#34A853"
                      d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z"
                    />
                    <path
                      fill="#FBBC05"
                      d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l2.85-2.22.81-.62z"
                    />
                    <path
                      fill="#EA4335"
                      d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z"
                    />
                  </svg>
                  {t('auth.login.googleLogin') || 'Sign in with Google'}
                </Button>
              )}
            </div>
          </CardContent>

          <CardFooter className="flex justify-center">
            <p className="text-sm text-gray-600">
              {t('auth.login.noAccount')}{' '}
              <Link to="/register" className="text-blue-600 hover:underline font-medium">
                {t('auth.login.registerNow')}
              </Link>
            </p>
          </CardFooter>
        </Card>

        <p className="text-center text-sm text-gray-500 mt-8">
          © 2024 {t('common.appName')}. All rights reserved.
        </p>
      </div>

      {/* Google 登录人机验证弹窗 / Google login CAPTCHA modal */}
      <Dialog open={showGoogleCaptchaModal} onOpenChange={setShowGoogleCaptchaModal}>
        <DialogContent className="sm:max-w-sm">
          <DialogHeader>
            <DialogTitle>{t('auth.captcha.modalTitle')}</DialogTitle>
            <DialogDescription>{t('auth.captcha.modalDesc')}</DialogDescription>
          </DialogHeader>
          <div className="flex justify-center py-4">
            <SliderCaptcha
              key={`google-${captchaKey}`}
              onVerified={handleGoogleCaptchaVerified}
              onError={() => setError(t('auth.captcha.required'))}
            />
          </div>
        </DialogContent>
      </Dialog>
    </div>
  );
}
