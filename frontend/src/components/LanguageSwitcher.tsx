import { Globe, Check } from 'lucide-react';
import { Button } from '@/components/ui/button';
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu';
import { useLanguageStore } from '@/store/language.store';
import { useTranslation } from 'react-i18next';

export function LanguageSwitcher() {
  const { lng, setLng } = useLanguageStore();
  const { t } = useTranslation();

  const languages = [
    { code: 'en', label: t('language.en') },
    { code: 'zh-CN', label: t('language.zhCN') },
    { code: 'zh-TW', label: t('language.zhTW') },
  ];

  return (
    <DropdownMenu>
      <DropdownMenuTrigger asChild>
        <Button variant="ghost" size="icon" className="relative">
          <Globe className="w-4 h-4" />
          <span className="sr-only">{t('language.switch')}</span>
        </Button>
      </DropdownMenuTrigger>
      <DropdownMenuContent align="end">
        {languages.map((lang) => (
          <DropdownMenuItem
            key={lang.code}
            className="cursor-pointer flex items-center justify-between min-w-[140px]"
            onClick={() => setLng(lang.code)}
          >
            <span>{lang.label}</span>
            {lng === lang.code && <Check className="w-4 h-4 ml-2" />}
          </DropdownMenuItem>
        ))}
      </DropdownMenuContent>
    </DropdownMenu>
  );
}
