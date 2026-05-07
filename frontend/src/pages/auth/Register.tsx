import { useState, useMemo } from 'react';
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
import { FileText, Loader2, Eye, EyeOff } from 'lucide-react';
import type { AxiosError } from 'axios';
import { LanguageSwitcher } from '@/components/LanguageSwitcher';

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

function useRegisterSchema() {
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
          agreeTerms: z.boolean().refine((val) => val === true, {
            message: t('auth.register.errors.agreeTermsRequired'),
          }),
        })
        .refine((data) => data.password === data.confirmPassword, {
          message: t('auth.register.errors.passwordMismatch'),
          path: ['confirmPassword'],
        }),
    [t]
  );
}

type RegisterFormValues = z.infer<ReturnType<typeof useRegisterSchema>>;

export default function Register() {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const { register, loginByGoogle } = useAuth();
  const [showPassword, setShowPassword] = useState(false);
  const [showConfirmPassword, setShowConfirmPassword] = useState(false);
  const [error, setError] = useState('');
  const [googleLoading, setGoogleLoading] = useState(false);

  const registerSchema = useRegisterSchema();

  const form = useForm<RegisterFormValues>({
    resolver: zodResolver(registerSchema),
    defaultValues: {
      email: '',
      password: '',
      confirmPassword: '',
      agreeTerms: false,
    },
  });

  const { isSubmitting } = form.formState;
  const passwordValue = useWatch({ control: form.control, name: 'password' });
  const strength = useMemo(() => getPasswordStrength(passwordValue || ''), [passwordValue]);

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
    try {
      await register({ email: values.email, password: values.password }, false);
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
      await loginByGoogle(credentialResponse.credential, false);
      navigate('/resumes', { replace: true });
    } catch {
      setError(t('auth.login.googleLoginFailed'));
    } finally {
      setGoogleLoading(false);
    }
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

                <Button type="submit" className="w-full h-11" disabled={isSubmitting}>
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
              ) : (
                <GoogleLogin
                  onSuccess={handleGoogleSuccess}
                  onError={() => setError(t('auth.login.googleLoginFailed'))}
                  useOneTap
                />
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
    </div>
  );
}
