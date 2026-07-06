/**
 * Formats a raw token count into a human-readable string.
 * 将原始 token 计数格式化为人类可读的字符串。
 *
 * - ≥ 1e6 → "1.2M"
 * - ≥ 1e3 → "199.3k"
 * - < 1e3 → raw number as string
 */
export function formatTokens(tokens: number): string {
  if (tokens >= 1_000_000) {
    const millions = tokens / 1_000_000;
    return millions >= 100 ? `${Math.round(millions)}M` : `${millions.toFixed(1)}M`;
  }
  if (tokens >= 1_000) {
    const thousands = tokens / 1_000;
    return thousands >= 100 ? `${Math.round(thousands)}k` : `${thousands.toFixed(1)}k`;
  }
  return String(tokens);
}
