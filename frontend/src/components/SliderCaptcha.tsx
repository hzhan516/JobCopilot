import { useState, useRef, useEffect, useCallback } from 'react';
import { useTranslation } from 'react-i18next';
import { authService } from '@/services/api';
import { Check, X, Loader2 } from 'lucide-react';

interface SliderCaptchaProps {
  onVerified: (captchaToken: string) => void;
  onError?: () => void;
  width?: number;
  className?: string;
}

/**
 * 滑动验证码组件 / Slider CAPTCHA component
 *
 * 用户拖动滑块到目标位置完成人机验证。
 * 使用 Pointer Events 统一支持鼠标、触摸屏和触控笔。
 */
export default function SliderCaptcha({
  onVerified,
  onError,
  width = 300,
  className = '',
}: SliderCaptchaProps) {
  const { t } = useTranslation();
  const trackRef = useRef<HTMLDivElement>(null);

  const [challenge, setChallenge] = useState<{ captchaId: string; targetX: number } | null>(null);
  const [offsetX, setOffsetX] = useState(0);
  const [isDragging, setIsDragging] = useState(false);
  const [isVerifying, setIsVerifying] = useState(false);
  const [status, setStatus] = useState<'idle' | 'success' | 'error'>('idle');

  const trackWidth = width;
  const handleWidth = 40;
  const maxOffset = trackWidth - handleWidth;

  const loadChallenge = useCallback(async () => {
    try {
      const result = await authService.getCaptchaChallenge();
      setChallenge(result);
      setOffsetX(0);
      setStatus('idle');
    } catch {
      onError?.();
    }
  }, [onError]);

  useEffect(() => {
    loadChallenge();
  }, [loadChallenge]);

  const handlePointerDown = (e: React.PointerEvent<HTMLDivElement>) => {
    if (isVerifying || status === 'success') return;
    e.preventDefault();
    setIsDragging(true);
    (e.currentTarget as HTMLDivElement).setPointerCapture(e.pointerId);
  };

  const handlePointerMove = (e: React.PointerEvent<HTMLDivElement>) => {
    if (!isDragging || !trackRef.current) return;
    e.preventDefault();
    const rect = trackRef.current.getBoundingClientRect();
    const x = e.clientX - rect.left - handleWidth / 2;
    const clamped = Math.max(0, Math.min(x, maxOffset));
    setOffsetX(clamped);
  };

  const handlePointerUp = async (e: React.PointerEvent<HTMLDivElement>) => {
    if (!isDragging) return;
    e.preventDefault();
    setIsDragging(false);
    (e.currentTarget as HTMLDivElement).releasePointerCapture(e.pointerId);

    if (isVerifying || !challenge) return;

    // verify 请求未完成时禁用拖动，防止暴力请求
    // Disable dragging while verify request is pending to prevent brute-force requests
    setIsVerifying(true);
    try {
      const centerX = Math.round(offsetX + handleWidth / 2);
      const result = await authService.verifyCaptcha({
        captchaId: challenge.captchaId,
        offsetX: centerX,
      });
      setStatus('success');
      onVerified(result.captchaToken);
    } catch {
      setStatus('error');
      setOffsetX(0);
      // 短暂显示错误状态后重新加载挑战
      // Reload challenge after briefly showing error state
      setTimeout(() => {
        loadChallenge();
      }, 800);
      onError?.();
    } finally {
      setIsVerifying(false);
    }
  };

  const getStatusText = () => {
    if (status === 'success') return t('auth.captcha.success');
    if (status === 'error') return t('auth.captcha.fail');
    return t('auth.captcha.dragHint');
  };

  const getTrackBg = () => {
    if (status === 'success') return 'bg-green-100';
    if (status === 'error') return 'bg-red-100';
    return 'bg-gray-100';
  };

  const getHandleBg = () => {
    if (status === 'success') return 'bg-green-500 text-white border-green-500';
    if (status === 'error') return 'bg-red-500 text-white border-red-500';
    return 'bg-white text-gray-600 border-gray-200 hover:bg-gray-50';
  };

  const getTargetBorder = () => {
    if (status === 'success') return 'border-green-400 bg-green-200/50';
    return 'border-blue-400 bg-blue-200/30';
  };

  if (!challenge) {
    return (
      <div
        role="presentation"
        className={`h-10 bg-gray-100 rounded-lg animate-pulse ${className}`}
        style={{ width: trackWidth }}
      />
    );
  }

  return (
    <div className={`select-none ${className}`} style={{ width: trackWidth }}>
      <div className="text-sm text-gray-500 mb-1.5 text-center transition-colors duration-300">
        {getStatusText()}
      </div>
      <div
        ref={trackRef}
        className={`relative h-10 rounded-lg overflow-hidden transition-colors duration-300 ${getTrackBg()}`}
        style={{ width: trackWidth }}
      >
        {/* 目标标记 / Target marker */}
        <div
          className={`absolute top-1 bottom-1 w-10 rounded-md border-2 border-dashed transition-colors duration-300 ${getTargetBorder()}`}
          style={{ left: challenge.targetX - handleWidth / 2 }}
        />

        {/* 滑块 / Handle */}
        <div
          className={`absolute top-0 h-10 w-10 rounded-lg shadow-sm flex items-center justify-center border transition-colors duration-300 ${getHandleBg()}`}
          style={{
            left: offsetX,
            touchAction: 'none',
            cursor: isVerifying || status === 'success' ? 'default' : 'grab',
          }}
          onPointerDown={handlePointerDown}
          onPointerMove={handlePointerMove}
          onPointerUp={handlePointerUp}
        >
          {isVerifying ? (
            <Loader2 className="w-4 h-4 animate-spin" />
          ) : status === 'success' ? (
            <Check className="w-4 h-4" />
          ) : status === 'error' ? (
            <X className="w-4 h-4" />
          ) : (
            <span className="text-xs font-medium">→</span>
          )}
        </div>
      </div>
    </div>
  );
}
