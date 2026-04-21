import { useState, useMemo } from 'react';
import { Link, useNavigate } from 'react-router-dom';
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

const registerSchema = z
  .object({
    email: z.string().min(1, '请输入邮箱地址').email('请输入有效的邮箱地址'),
    password: z.string().min(1, '请输入密码').min(6, '密码长度至少为6位').max(32, '密码长度最多为32位'),
    confirmPassword: z.string().min(1, '请确认密码'),
    agreeTerms: z.boolean().refine((val) => val === true, {
      message: '请阅读并同意服务条款',
    }),
  })
  .refine((data) => data.password === data.confirmPassword, {
    message: '两次输入的密码不一致',
    path: ['confirmPassword'],
  });

type RegisterFormValues = z.infer<typeof registerSchema>;

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

const strengthConfig: Record<
  Strength,
  { label: string; color: string; barColor: string }
> = {
  weak: { label: '弱', color: 'text-red-600', barColor: 'bg-red-500' },
  medium: { label: '中', color: 'text-yellow-600', barColor: 'bg-yellow-500' },
  strong: { label: '强', color: 'text-green-600', barColor: 'bg-green-500' },
};

export default function Register() {
  const navigate = useNavigate();
  const { register } = useAuth();
  const [showPassword, setShowPassword] = useState(false);
  const [showConfirmPassword, setShowConfirmPassword] = useState(false);
  const [error, setError] = useState('');

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
        setError('邮箱已被注册');
      } else if (status === 400) {
        setError(message || '参数校验失败，请检查输入内容');
      } else {
        setError(message || '注册失败，请稍后重试');
      }
    }
  };

  return (
    <div className="min-h-screen flex items-center justify-center bg-gradient-to-br from-blue-50 to-indigo-100 px-4">
      <div className="w-full max-w-md">
        {/* Logo */}
        <div className="flex justify-center mb-8">
          <div className="flex items-center space-x-3">
            <div className="w-12 h-12 bg-blue-600 rounded-xl flex items-center justify-center shadow-lg">
              <FileText className="w-7 h-7 text-white" />
            </div>
            <span className="text-2xl font-bold text-gray-900">智能求职助手</span>
          </div>
        </div>

        <Card className="shadow-xl border-0">
          <CardHeader className="space-y-1">
            <CardTitle className="text-2xl font-bold text-center">创建账户</CardTitle>
            <CardDescription className="text-center">开始您的智能求职之旅</CardDescription>
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
                      <FormLabel>邮箱地址</FormLabel>
                      <FormControl>
                        <Input
                          type="email"
                          placeholder="your@email.com"
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
                      <FormLabel>密码</FormLabel>
                      <FormControl>
                        <div className="relative">
                          <Input
                            type={showPassword ? 'text' : 'password'}
                            placeholder="6-32位字符"
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
                              {strengthInfo.label}
                            </span>
                          </div>
                          <p className="text-xs text-gray-500">
                            建议包含大小写字母、数字和特殊符号，长度 8 位以上
                          </p>
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
                      <FormLabel>确认密码</FormLabel>
                      <FormControl>
                        <div className="relative">
                          <Input
                            type={showConfirmPassword ? 'text' : 'password'}
                            placeholder="再次输入密码"
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
                          我已阅读并同意
                          <Link to="#" className="text-blue-600 hover:underline ml-1">
                            服务条款
                          </Link>
                          和
                          <Link to="#" className="text-blue-600 hover:underline ml-1">
                            隐私政策
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
                      注册中...
                    </>
                  ) : (
                    '注册'
                  )}
                </Button>
              </form>
            </Form>
          </CardContent>

          <CardFooter className="flex justify-center">
            <p className="text-sm text-gray-600">
              已有账户？{' '}
              <Link to="/login" className="text-blue-600 hover:underline font-medium">
                立即登录
              </Link>
            </p>
          </CardFooter>
        </Card>

        {/* 页脚 */}
        <p className="text-center text-sm text-gray-500 mt-8">
          © 2024 智能求职助手. All rights reserved.
        </p>
      </div>
    </div>
  );
}
