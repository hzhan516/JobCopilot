import { Input } from '@/components/ui/input';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select';
import { Search, Filter } from 'lucide-react';

interface SortOption {
  value: string;
  label: string;
}

interface MatchFilterOption {
  value: string;
  label: string;
}

interface JobFilterBarProps {
  searchQuery: string;
  sortBy: string;
  searchPlaceholder: string;
  sortLabel: string;
  sortOptions: SortOption[];
  matchFilter?: string;
  matchFilterLabel?: string;
  matchFilterOptions?: MatchFilterOption[];
  onSearchChange: (value: string) => void;
  onSortChange: (value: string) => void;
  onMatchFilterChange?: (value: string) => void;
}

export default function JobFilterBar({
  searchQuery,
  sortBy,
  searchPlaceholder,
  sortLabel,
  sortOptions,
  matchFilter,
  matchFilterLabel,
  matchFilterOptions,
  onSearchChange,
  onSortChange,
  onMatchFilterChange,
}: JobFilterBarProps) {
  return (
    <div className="flex flex-col lg:flex-row gap-4">
      <div className="relative flex-1">
        <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" />
        <Input
          placeholder={searchPlaceholder}
          value={searchQuery}
          onChange={(e) => onSearchChange(e.target.value)}
          className="pl-10"
        />
      </div>
      <div className="flex gap-4">
        {onMatchFilterChange && matchFilterOptions && (
          <Select value={matchFilter} onValueChange={onMatchFilterChange}>
            <SelectTrigger className="w-44">
              <Filter className="w-4 h-4 mr-2" />
              <SelectValue placeholder={matchFilterLabel} />
            </SelectTrigger>
            <SelectContent>
              {matchFilterOptions.map((option) => (
                <SelectItem key={option.value} value={option.value}>
                  {option.label}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
        )}
        <Select value={sortBy} onValueChange={onSortChange}>
          <SelectTrigger className="w-40">
            <Filter className="w-4 h-4 mr-2" />
            <SelectValue placeholder={sortLabel} />
          </SelectTrigger>
          <SelectContent>
            {sortOptions.map((option) => (
              <SelectItem key={option.value} value={option.value}>
                {option.label}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
      </div>
    </div>
  );
}
