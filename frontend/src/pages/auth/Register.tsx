import { useState, useMemo, useEffect, useCallback } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { GoogleLogin } from '@react-oauth/google';
import { useAuth } from '@/hooks/useAuth';
import { z } from 'zod';
import { useForm, useWatch } from 'react-hook-form';
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
import type { AxiosError } from 'axios';
import { LanguageSwitcher } from '@/components/LanguageSwitcher';
import { authService } from '@/services/api';

type Strength = 'weak' | 'medium' | 'strong';

function getPasswordStrength(password: string): Strength {
  if (!password || password.length < 6) return 'weak';
  let score = 0;
  if (password.length >= 8) score++;
  if (password.length >= 10) score++;
  if (/[a-z]/.test(password)) score++;
  if (/[A-Z]/.test(password)) score++;
  if (/\d/.test(password)) score++;
  if (/[^A-Za-z0-9]/.test(password)) score++;

  if (score <= 2) return 'weak';
  if (score <= 4) return 'medium';
  return 'strong';
}

function useRegisterSchema(emailVerificationEnabled: boolean) {
  const { t } = useTranslation();
  return useMemo(
    () =>
      z
        .object({
          email: z.string().min(1, t('auth.register.errors.emailRequired')).email(t('auth.register.errors.emailInvalid')),
          password: z
            .string()
            .min(1, t('auth.register.errors.passwordRequired'))
            .min(6, t('auth.register.errors.passwordMin'))
            .max(32, t('auth.register.errors.passwordMax')),
          confirmPassword: z.string().min(1, t('auth.register.errors.confirmPasswordRequired')),
          agreeTerms: z.boolean().refine((val) => val, {
            message: t('auth.register.errors.agreeTermsRequired'),
          }),
          verificationCode: emailVerificationEnabled
            ? z.string().min(1, t('auth.register.errors.verificationCodeRequired')).regex(/^\d{6}$/, t('auth.register.errors.verificationCodeInvalid'))
            : z.string().optional(),
        })
        .refine((data) => data.password === data.confirmPassword, {
          message: t('auth.register.errors.passwordMismatch'),
          path: ['confirmPassword'],
        }),
    [t, emailVerificationEnabled]
  );
}

type RegisterFormValues = z.infer<ReturnType<typeof useRegisterSchema>>;

const COOLDOWN_SECONDS = 60;
const STORAGE_KEY = 'verificationCodeSentAt';

export default function Register() {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const { register, loginByGoogle } = useAuth();
  const [showPassword, setShowPassword] = useState(false);
  const [showConfirmPassword, setShowConfirmPassword] = useState(false);
  const [error, setError] = useState('');
  const [googleLoading, setGoogleLoading] = useState(false);
  const [emailVerificationEnabled, setEmailVerificationEnabled] = useState(false);
  const [countdown, setCountdown] = useState(0);
  const [isSendingCode, setIsSendingCode] = useState(false);
  const [sendError, setSendError] = useState('');

  // 人机验证状态 / CAPTCHA state
  const [captchaToken, setCaptchaToken] = useState('');
  const [captchaKey, setCaptchaKey] = useState(0);
  const [showGoogleCaptchaModal, setShowGoogleCaptchaModal] = useState(false);
  const [googleCaptchaVerified, setGoogleCaptchaVerified] = useState(false);

  const registerSchema = useRegisterSchema(emailVerificationEnabled);

  const form = useForm<RegisterFormValues>({
    resolver: zodResolver(registerSchema),
    defaultValues: {
      email: '',
      password: '',
      confirmPassword: '',
      agreeTerms: false,
      verificationCode: '',
    },
  });

  const { isSubmitting } = form.formState;
  const passwordValue = useWatch({ control: form.control, name: 'password' });
  const strength = useMemo(() => getPasswordStrength(passwordValue || ''), [passwordValue]);

  // 恢复倒计时状态（页面刷新后） / Restore countdown state after page refresh
  useEffect(() => {
    const sentAt = sessionStorage.getItem(STORAGE_KEY);
    if (sentAt) {
      const elapsed = Math.floor((Date.now() - parseInt(sentAt, 10)) / 1000);
      const remaining = COOLDOWN_SECONDS - elapsed;
      if (remaining > 0) {
        setCountdown(remaining);
      } else {
        sessionStorage.removeItem(STORAGE_KEY);
      }
    }
  }, []);

  // 倒计时逻辑 / Countdown logic
  const isCountingDown = countdown > 0;
  useEffect(() => {
    if (!isCountingDown) return;
    const timer = setInterval(() => {
      setCountdown((prev) => {
        if (prev <= 1) {
          sessionStorage.removeItem(STORAGE_KEY);
          clearInterval(timer);
          return 0;
        }
        return prev - 1;
      });
    }, 1000);
    return () => clearInterval(timer);
  }, [isCountingDown]);

  // 查询邮箱验证开关状态 / Check email verification toggle
  useEffect(() => {
    authService.isEmailVerificationEnabled().then((enabled) => {
      setEmailVerificationEnabled(enabled);
    }).catch(() => {
      setEmailVerificationEnabled(false);
    });
  }, []);

  const resetCaptcha = useCallback(() => {
    setCaptchaToken('');
    setCaptchaKey((prev) => prev + 1);
    setGoogleCaptchaVerified(false);
    setShowGoogleCaptchaModal(false);
  }, []);

  const strengthConfig: Record<
    Strength,
    { labelKey: string; color: string; barColor: string }
  > = {
    weak: { labelKey: 'auth.register.passwordStrength.weak', color: 'text-red-600', barColor: 'bg-red-500' },
    medium: { labelKey: 'auth.register.passwordStrength.medium', color: 'text-yellow-600', barColor: 'bg-yellow-500' },
    strong: { labelKey: 'auth.register.passwordStrength.strong', color: 'text-green-600', barColor: 'bg-green-500' },
  };

  const strengthInfo = strengthConfig[strength];

  const onSubmit = async (values: RegisterFormValues) => {
    setError('');
    setSendError('');

    if (!captchaToken) {
      setError(t('auth.captcha.required'));
      return;
    }

    try {
      const payload: { email: string; password: string; verificationCode?: string; captchaToken: string } = {
        email: values.email,
        password: values.password,
        captchaToken,
      };
      if (emailVerificationEnabled && values.verificationCode) {
        payload.verificationCode = values.verificationCode;
      }
      await register(payload, false);
      navigate('/resumes', { replace: true });
    } catch (err) {
      const axiosErr = err as AxiosError<{ message?: string }>;
      const status = axiosErr.response?.status;
      const message = axiosErr.response?.data?.message || axiosErr.message;

      if (status === 409) {
        setError(t('auth.register.errorEmailExists'));
      } else if (status === 400) {
        setError(message || t('auth.register.errorValidation'));
      } else {
        setError(message || t('auth.register.errorGeneric'));
      }
      // 任何业务错误都重置人机验证，因为后端 token 可能已被消耗
      // Reset CAPTCHA on any business error because the backend token may have been consumed
      resetCaptcha();
    }
  };

  const doSendCode = useCallback(async (email: string, token: string) => {
    setIsSendingCode(true);
    try {
      await authService.sendVerificationCode({ email, captchaToken: token });
      sessionStorage.setItem(STORAGE_KEY, Date.now().toString());
      setCountdown(COOLDOWN_SECONDS);
    } catch (err) {
      const axiosErr = err as AxiosError<{ message?: string }>;
      const message = axiosErr.response?.data?.message || axiosErr.message;
      setSendError(message || t('auth.register.errors.sendCodeFailed'));
      resetCaptcha();
    } finally {
      setIsSendingCode(false);
    }
  }, [t, resetCaptcha]);

  const handleCaptchaVerifiedForCode = useCallback((token: string) => {
    setCaptchaToken(token);
    const email = form.getValues('email');
    // 自动发送验证码 / Auto-send verification code after CAPTCHA verification
    if (email) {
      doSendCode(email, token);
    }
  }, [form, doSendCode]);

  const handleSendCode = useCallback(async () => {
    setSendError('');
    setError('');

    const emailResult = await form.trigger('email');
    if (!emailResult) return;

    const email = form.getValues('email');

    if (!captchaToken) {
      setError(t('auth.captcha.required'));
      return;
    }

    doSendCode(email, captchaToken);
  }, [form, captchaToken, t, doSendCode]);

  const handleGoogleSuccess = async (credentialResponse: { credential?: string }) => {
    if (!credentialResponse.credential) {
      setError(t('auth.login.googleLoginFailed'));
      return;
    }
    if (!captchaToken || !googleCaptchaVerified) {
      setError(t('auth.captcha.required'));
      setGoogleCaptchaVerified(false);
      setShowGoogleCaptchaModal(true);
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
            <CardTitle className="text-2xl font-bold text-center">{t('auth.register.title')}</CardTitle>
            <CardDescription className="text-center">{t('auth.register.subtitle')}</CardDescription>
          </CardHeader>

          <CardContent>
            {(error || sendError) && (
              <Alert variant="destructive" className="mb-4">
                <AlertDescription>{error || sendError}</AlertDescription>
              </Alert>
            )}

            <Form {...form}>
              <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-4">
                <FormField
                  control={form.control}
                  name="email"
                  render={({ field }) => (
                    <FormItem>
                      <FormLabel>{t('auth.register.emailLabel')}</FormLabel>
                      <FormControl>
                        <Input
                          type="email"
                          placeholder={t('auth.register.emailPlaceholder')}
                          disabled={isSubmitting}
                          className="h-11"
                          {...field}
                        />
                      </FormControl>
                      <FormMessage />
                    </FormItem>
                  )}
                />

                {emailVerificationEnabled && (
                  <FormField
                    control={form.control}
                    name="verificationCode"
                    render={({ field }) => (
                      <FormItem>
                        <FormLabel>{t('auth.register.verificationCodeLabel')}</FormLabel>
                        <FormControl>
                          <div className="flex gap-2">
                            <Input
                              type="text"
                              placeholder={t('auth.register.verificationCodePlaceholder')}
                              disabled={isSubmitting}
                              maxLength={6}
                              className="h-11 flex-1"
                              {...field}
                            />
                            <Button
                              type="button"
                              variant="outline"
                              onClick={handleSendCode}
                              disabled={countdown > 0 || isSendingCode || !captchaToken}
                              className="h-11 whitespace-nowrap px-4"
                            >
                              {isSendingCode ? (
                                <Loader2 className="w-4 h-4 animate-spin" />
                              ) : countdown > 0 ? (
                                `${countdown}s`
                              ) : (
                                t('auth.register.sendCode')
                              )}
                            </Button>
                          </div>
                        </FormControl>
                        <FormMessage />
                      </FormItem>
                    )}
                  />
                )}

                {/* 注册需要验证码时：人机验证在发送验证码前 / When email verification is required: CAPTCHA before sending code */}
                {emailVerificationEnabled && (
                  <SliderCaptcha
                    key={`code-${captchaKey}`}
                    onVerified={handleCaptchaVerifiedForCode}
                    onError={() => setError(t('auth.captcha.required'))}
                    className="mx-auto"
                  />
                )}

                <FormField
                  control={form.control}
                  name="password"
                  render={({ field }) => (
                    <FormItem>
                      <FormLabel>{t('auth.register.passwordLabel')}</FormLabel>
                      <FormControl>
                        <div className="relative">
                          <Input
                            type={showPassword ? 'text' : 'password'}
                            placeholder={t('auth.register.passwordPlaceholder')}
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
                      {field.value && (
                        <div className="mt-2 space-y-1">
                          <div className="flex items-center space-x-2">
                            <div className="flex-1 h-1.5 bg-gray-200 rounded-full overflow-hidden">
                              <div
                                className={`h-full transition-all duration-300 ${strengthInfo.barColor}`}
                                style={{
                                  width:
                                    strength === 'weak' ? '33%' : strength === 'medium' ? '66%' : '100%',
                                }}
                              />
                            </div>
                            <span className={`text-xs font-medium ${strengthInfo.color}`}>
                              {t(strengthInfo.labelKey)}
                            </span>
                          </div>
                          <p className="text-xs text-gray-500">{t('auth.register.passwordStrength.hint')}</p>
                        </div>
                      )}
                      <FormMessage />
                    </FormItem>
                  )}
                />

                <FormField
                  control={form.control}
                  name="confirmPassword"
                  render={({ field }) => (
                    <FormItem>
                      <FormLabel>{t('auth.register.confirmPasswordLabel')}</FormLabel>
                      <FormControl>
                        <div className="relative">
                          <Input
                            type={showConfirmPassword ? 'text' : 'password'}
                            placeholder={t('auth.register.confirmPasswordPlaceholder')}
                            disabled={isSubmitting}
                            className="h-11 pr-10"
                            {...field}
                          />
                          <button
                            type="button"
                            onClick={() => setShowConfirmPassword(!showConfirmPassword)}
                            className="absolute right-3 top-1/2 -translate-y-1/2 text-gray-500 hover:text-gray-700"
                            tabIndex={-1}
                          >
                            {showConfirmPassword ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                          </button>
                        </div>
                      </FormControl>
                      <FormMessage />
                    </FormItem>
                  )}
                />

                <FormField
                  control={form.control}
                  name="agreeTerms"
                  render={({ field }) => (
                    <FormItem className="flex flex-row items-start space-x-3 space-y-0 py-1">
                      <FormControl>
                        <Checkbox checked={field.value} onCheckedChange={field.onChange} disabled={isSubmitting} />
                      </FormControl>
                      <div className="space-y-1 leading-none">
                        <FormLabel className="font-normal cursor-pointer">
                          {t('auth.register.agreeTerms')}
                          <Link to="#" className="text-blue-600 hover:underline ml-1">
                            {t('auth.register.termsOfService')}
                          </Link>
                          {t('auth.register.and')}
                          <Link to="#" className="text-blue-600 hover:underline ml-1">
                            {t('auth.register.privacyPolicy')}
                          </Link>
                        </FormLabel>
                        <FormMessage />
                      </div>
                    </FormItem>
                  )}
                />

                {/* 注册不需要验证码时：人机验证在注册按钮前 / When no email verification: CAPTCHA before register button */}
                {!emailVerificationEnabled && (
                  <SliderCaptcha
                    key={`register-${captchaKey}`}
                    onVerified={(token) => setCaptchaToken(token)}
                    onError={() => setError(t('auth.captcha.required'))}
                    className="mx-auto"
                  />
                )}

                <Button type="submit" className="w-full h-11" disabled={isSubmitting || !captchaToken}>
                  {isSubmitting ? (
                    <>
                      <Loader2 className="w-4 h-4 mr-2 animate-spin" />
                      {t('auth.register.registering')}
                    </>
                  ) : (
                    t('auth.register.registerButton')
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
              {t('auth.register.hasAccount')}{' '}
              <Link to="/login" className="text-blue-600 hover:underline font-medium">
                {t('auth.register.loginNow')}
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
