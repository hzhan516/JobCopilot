import { useTranslation } from 'react-i18next';
import { Badge } from '../ui/badge';
import { Loader2, CheckCircle, XCircle, Clock } from 'lucide-react';
import type { ResumeVersion } from '../../types/resume';

interface ParseStatusBadgeProps {
  status: ResumeVersion['parseStatus'];
}

export function ParseStatusBadge({ status }: ParseStatusBadgeProps) {
  const { t } = useTranslation();

  switch (status) {
    case 'PENDING':
      return (
        <Badge variant="secondary" className="flex items-center gap-1 w-fit">
          <Clock className="w-3 h-3" />
          {t('resume.parseStatus.pending')}
        </Badge>
      );
    case 'PARSING':
      return (
        <Badge variant="secondary" className="flex items-center gap-1 w-fit bg-blue-100 text-blue-800 hover:bg-blue-100 dark:bg-blue-900 dark:text-blue-300">
          <Loader2 className="w-3 h-3 animate-spin" />
          {t('resume.parseStatus.parsing')}
        </Badge>
      );
    case 'COMPLETED':
      return (
        <Badge variant="secondary" className="flex items-center gap-1 w-fit bg-green-100 text-green-800 hover:bg-green-100 dark:bg-green-900 dark:text-green-300">
          <CheckCircle className="w-3 h-3" />
          {t('resume.parseStatus.completed')}
        </Badge>
      );
    case 'FAILED':
      return (
        <Badge variant="destructive" className="flex items-center gap-1 w-fit">
          <XCircle className="w-3 h-3" />
          {t('resume.parseStatus.failed')}
        </Badge>
      );
    case 'NOT_APPLICABLE':
      return null;
    default:
      return null;
  }
}
