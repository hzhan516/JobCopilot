import { Button } from '@/components/ui/button';
import { Plus } from 'lucide-react';

interface JobListHeaderProps {
  title: string;
  subtitle: string;
  addButtonLabel: string;
  onAddClick: () => void;
}

/**
 * 职位列表页面头部
 * Job list page header
 */
export default function JobListHeader({
  title,
  subtitle,
  addButtonLabel,
  onAddClick,
}: JobListHeaderProps) {
  return (
    <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
      <div>
        <h1 className="text-3xl font-bold text-gray-900">{title}</h1>
        <p className="text-gray-500 mt-1">{subtitle}</p>
      </div>
      <Button onClick={onAddClick}>
        <Plus className="w-4 h-4 mr-2" />
        {addButtonLabel}
      </Button>
    </div>
  );
}
