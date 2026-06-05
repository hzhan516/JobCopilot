import { useTranslation } from 'react-i18next';
import { formatDate } from '@/utils/i18n';
import { Card, CardContent, CardDescription, CardFooter, CardHeader, CardTitle } from '../ui/card';
import { Badge } from '../ui/badge';
import { Button } from '../ui/button';
import { FileText, Trash2, Eye } from 'lucide-react';
import type { ResumeGroup } from '../../types/resume';
import { ParseStatusBadge } from './ParseStatusBadge';

interface ResumeCardProps {
  group: ResumeGroup;
  onView: (groupId: string) => void;
  onDelete: (groupId: string) => void;
}

export function ResumeCard({ group, onView, onDelete }: ResumeCardProps) {
  const { t } = useTranslation();
  const latestVersion = group.versions.length > 0 
    ? group.versions.reduce((latest, current) => 
        new Date(current.createdAt) > new Date(latest.createdAt) ? current : latest
      )
    : null;
  const aiParseVersion = group.versions.find((version) => version.versionType === 'ORIGINAL') ?? latestVersion;

  const formattedDate = formatDate(group.createdAt, {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
  });

  return (
    <Card className="flex flex-col h-full hover:shadow-md transition-shadow">
      <CardHeader className="pb-3">
        <div className="flex justify-between items-start gap-4">
          <div className="flex items-center gap-2">
            <div className="p-2 bg-primary/10 rounded-md">
              <FileText className="w-5 h-5 text-primary" />
            </div>
            <div>
              <CardTitle className="text-lg line-clamp-1" title={group.title}>
                {group.title}
              </CardTitle>
              <CardDescription className="text-xs mt-1">
                {t('resume.card.created')} {formattedDate}
              </CardDescription>
            </div>
          </div>
          {group.isDefault && (
            <Badge variant="default" className="shrink-0">{t('resume.card.default')}</Badge>
          )}
        </div>
      </CardHeader>
      <CardContent className="flex-grow pb-3">
        <div className="flex flex-col gap-2 text-sm">
          <div className="flex justify-between items-center">
            <span className="text-muted-foreground">{t('resume.card.versions')}</span>
            <span className="font-medium">{group.versions.length}</span>
          </div>
          {aiParseVersion && aiParseVersion.parseStatus !== 'NOT_APPLICABLE' && (
            <div className="flex justify-between items-center">
              <span className="text-muted-foreground">{t('resume.card.latestStatus')}</span>
              <ParseStatusBadge status={aiParseVersion.parseStatus} />
            </div>
          )}
        </div>
      </CardContent>
      <CardFooter className="pt-3 border-t flex justify-between gap-2">
        <Button 
          variant="outline" 
          size="sm" 
          className="flex-1 flex items-center gap-1"
          onClick={() => onView(group.groupId)}
        >
          <Eye className="w-4 h-4" />
          {t('resume.card.viewDetails')}
        </Button>
        <Button 
          variant="ghost" 
          size="sm" 
          className="text-destructive hover:text-destructive hover:bg-destructive/10 px-2"
          onClick={() => onDelete(group.groupId)}
          title={t('resume.card.deleteTitle')}
        >
          <Trash2 className="w-4 h-4" />
        </Button>
      </CardFooter>
    </Card>
  );
}
