import { useTranslation } from 'react-i18next';
import { formatTokens } from '@/utils/formatTokens';
import { Button } from '@/components/ui/button';
import { Loader2, Minimize2 } from 'lucide-react';

export interface ContextUsageIndicatorProps {
  /** Prompt tokens from the most recent AI call / 最近一次 AI 调用的 prompt tokens */
  contextTokens?: number;
  /** Admin-configurable context window size / 管理员配置的上下文窗口大小 */
  contextWindow?: number;
  /** Pre-computed usage ratio (0.0–1.0) / 后端预计算的用量比例 */
  usageRatio?: number;
  /** Whether the usage ratio exceeds the compact threshold / 是否超过压缩阈值 */
  compactAdvised?: boolean;
  /** Whether compaction is in progress / 压缩是否正在进行中 */
  compacting?: boolean;
  /** Callback to trigger compaction / 触发压缩的回调 */
  onCompact?: () => void;
}

/**
 * Displays context usage as a small inline indicator: "20% 199.3k / 1000k used".
 * Shows a compact button when advised or when the user explicitly triggers it.
 * Gracefully degrades when token/window data is missing (old conversations).
 * 显示上下文用量指示器及可选的压缩按钮。token 数据缺失时优雅降级。
 */
export default function ContextUsageIndicator({
  contextTokens,
  contextWindow,
  usageRatio,
  compactAdvised,
  compacting = false,
  onCompact,
}: ContextUsageIndicatorProps) {
  const { t } = useTranslation();

  // ponytail: degrade silently when token data is unavailable (old conversations, no AI reply yet)
  if (!contextTokens || !contextWindow || contextTokens <= 0 || contextWindow <= 0) {
    return null;
  }

  const pct = usageRatio !== undefined ? Math.round(usageRatio * 100) : 0;

  return (
    <div className="flex items-center gap-1.5 text-[10px] text-muted-foreground px-3 py-1">
      <span>
        {t('chat.context.used', {
          percent: pct,
          used: formatTokens(contextTokens),
          total: formatTokens(contextWindow),
        })}
      </span>

      {compactAdvised && onCompact && (
        <Button
          variant="ghost"
          size="icon"
          className="h-5 w-5"
          disabled={compacting}
          onClick={onCompact}
          title={t('chat.context.compactHint')}
        >
          {compacting ? (
            <Loader2 className="w-3 h-3 animate-spin" />
          ) : (
            <Minimize2 className="w-3 h-3" />
          )}
        </Button>
      )}
    </div>
  );
}
