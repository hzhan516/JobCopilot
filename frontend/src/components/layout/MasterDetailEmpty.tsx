import { useTranslation } from 'react-i18next';
import type { LucideIcon } from 'lucide-react';

interface MasterDetailEmptyProps {
  /** 空状态图标 */
  icon: LucideIcon;
  /** 标题翻译键 */
  titleKey: string;
  /** 描述翻译键 */
  descKey: string;
}

/**
 * Master-Detail 详情栏未选中时的空状态占位。
 * 文案统一走 i18n，并使用 break-words 防止长文案/中日韩逐字纵排造成横向溢出。
 */
export default function MasterDetailEmpty({
  icon: Icon,
  titleKey,
  descKey,
}: MasterDetailEmptyProps) {
  const { t } = useTranslation();

  return (
    <div
      data-slot="master-detail-empty"
      className="h-full min-w-0 flex flex-col items-center justify-center text-center px-6 text-muted-foreground bg-muted/20"
    >
      <Icon className="w-16 h-16 mb-4 opacity-30" />
      <p className="text-lg font-medium break-words">{t(titleKey)}</p>
      <p className="text-sm mt-1 break-words">{t(descKey)}</p>
    </div>
  );
}
