import { Card, CardContent } from '@/components/ui/card';
import { Briefcase } from 'lucide-react';

interface JobEmptyStateProps {
  title: string;
  description: string;
}

/**
 * 职位列表空状态
 * Job list empty state
 */
export default function JobEmptyState({ title, description }: JobEmptyStateProps) {
  return (
    <Card className="border-dashed">
      <CardContent className="flex flex-col items-center justify-center py-16">
        <Briefcase className="w-16 h-16 text-gray-300 mb-4" />
        <h3 className="text-lg font-medium text-gray-900 mb-2">{title}</h3>
        <p className="text-gray-500">{description}</p>
      </CardContent>
    </Card>
  );
}
