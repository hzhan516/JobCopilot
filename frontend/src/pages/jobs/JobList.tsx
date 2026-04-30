import { useEffect, useState } from 'react';
import type { Job } from '@/types';
import { useTranslation } from 'react-i18next';
import { formatDate } from '@/utils/i18n';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { Skeleton } from '@/components/ui/skeleton';
import { Input } from '@/components/ui/input';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select';
import {
  Briefcase,
  MapPin,
  DollarSign,
  Calendar,
  Search,
  Filter,
  ExternalLink,
  Star,
} from 'lucide-react';
import { toast } from 'sonner';

// 模拟职位数据
const mockJobs: Job[] = [
  {
    jobId: '1',
    title: 'Senior Frontend Engineer',
    company: 'Google',
    location: 'Mountain View, CA',
    description: 'Responsible for front-end development of core products using React, TypeScript and other technologies.',
    requirements: ['3+ years front-end experience', 'Proficient in React and TypeScript', 'Large project experience'],
    salaryMin: 150000,
    salaryMax: 220000,
    postedAt: '2024-01-15T10:00:00',
    matchScore: 92,
  },
  {
    jobId: '2',
    title: 'Java Backend Developer',
    company: 'Amazon',
    location: 'Seattle, WA',
    description: 'Participate in back-end development of e-commerce platform using Spring Boot, MySQL, Redis, etc.',
    requirements: ['2+ years Java experience', 'Familiar with Spring Boot', 'Understanding of microservices'],
    salaryMin: 130000,
    salaryMax: 180000,
    postedAt: '2024-01-14T14:30:00',
    matchScore: 85,
  },
  {
    jobId: '3',
    title: 'Product Manager',
    company: 'Microsoft',
    location: 'Redmond, WA',
    description: 'Responsible for planning and design of social products, coordinating technical, design and operations teams.',
    requirements: ['3+ years product experience', 'Social product experience preferred', 'Good communication skills'],
    salaryMin: 140000,
    salaryMax: 200000,
    postedAt: '2024-01-13T09:00:00',
    matchScore: 78,
  },
  {
    jobId: '4',
    title: 'Data Analyst',
    company: 'Meta',
    location: 'Menlo Park, CA',
    description: 'Responsible for business data analysis and providing data support for product decisions.',
    requirements: ['Proficient in SQL', 'Master Python or R', 'Data analysis experience'],
    salaryMin: 120000,
    salaryMax: 160000,
    postedAt: '2024-01-12T16:00:00',
    matchScore: 72,
  },
  {
    jobId: '5',
    title: 'DevOps Engineer',
    company: 'Netflix',
    location: 'Los Gatos, CA',
    description: 'Responsible for CI/CD process construction, containerized deployment, and cloud platform operations.',
    requirements: ['Familiar with Docker, Kubernetes', 'Cloud platform experience', 'Familiar with Linux'],
    salaryMin: 140000,
    salaryMax: 190000,
    postedAt: '2024-01-11T11:00:00',
    matchScore: 68,
  },
];

export default function JobList() {
  const { t } = useTranslation();
  const [jobs, setJobs] = useState<Job[]>([]);
  const [filteredJobs, setFilteredJobs] = useState<Job[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [searchQuery, setSearchQuery] = useState('');
  const [locationFilter, setLocationFilter] = useState('all');
  const [sortBy, setSortBy] = useState('match');

  // 加载职位数据
  useEffect(() => {
    loadJobs();
  }, []);

  const loadJobs = async () => {
    try {
      setIsLoading(true);
      // 使用模拟数据，后续替换为真实API
      // const data = await jobService.getJobs();
      await new Promise((resolve) => setTimeout(resolve, 1000));
      setJobs(mockJobs);
      setFilteredJobs(mockJobs);
    } catch {
      toast.error(t('jobList.loadError'));
    } finally {
      setIsLoading(false);
    }
  };

  // 筛选和排序
  useEffect(() => {
    let result = [...jobs];

    // 搜索筛选
    if (searchQuery) {
      const query = searchQuery.toLowerCase();
      result = result.filter(
        (job) =>
          job.title.toLowerCase().includes(query) ||
          job.company.toLowerCase().includes(query) ||
          job.description.toLowerCase().includes(query)
      );
    }

    // 地点筛选
    if (locationFilter !== 'all') {
      result = result.filter((job) => job.location === locationFilter);
    }

    // 排序
    switch (sortBy) {
      case 'match':
        result.sort((a, b) => (b.matchScore || 0) - (a.matchScore || 0));
        break;
      case 'salary':
        result.sort((a, b) => (b.salaryMax || 0) - (a.salaryMax || 0));
        break;
      case 'date':
        result.sort((a, b) => new Date(b.postedAt).getTime() - new Date(a.postedAt).getTime());
        break;
    }

    setFilteredJobs(result);
  }, [jobs, searchQuery, locationFilter, sortBy]);

  // 获取所有地点
  const locations = Array.from(new Set(jobs.map((job) => job.location)));

  // 格式化薪资
  const formatSalary = (min?: number, max?: number) => {
    if (!min && !max) return t('jobList.salaryNegotiable');
    const format = (n: number) => (n / 1000).toFixed(0) + 'K';
    if (min && max) return t('jobList.salaryRange', { min: format(min), max: format(max) });
    if (min) return t('jobList.salaryMin', { min: format(min) });
    if (max) return t('jobList.salaryMax', { max: format(max) });
    return t('jobList.salaryNegotiable');
  };

  // 渲染骨架屏
  if (isLoading) {
    return (
      <div className="space-y-6">
        <div>
          <h1 className="text-3xl font-bold text-gray-900">{t('jobList.title')}</h1>
          <p className="text-gray-500 mt-1">{t('jobList.subtitle')}</p>
        </div>
        <div className="flex space-x-4">
          <Skeleton className="h-10 flex-1" />
          <Skeleton className="h-10 w-32" />
          <Skeleton className="h-10 w-32" />
        </div>
        <div className="grid gap-4">
          {[1, 2, 3, 4, 5].map((i) => (
            <Skeleton key={i} className="h-48" />
          ))}
        </div>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* 页面标题 */}
      <div>
        <h1 className="text-3xl font-bold text-gray-900">{t('jobList.title')}</h1>
        <p className="text-gray-500 mt-1">{t('jobList.subtitle')}</p>
      </div>

      {/* 筛选栏 */}
      <div className="flex flex-col lg:flex-row gap-4">
        <div className="relative flex-1">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" />
          <Input
            placeholder={t('jobList.searchPlaceholder')}
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            className="pl-10"
          />
        </div>
        <div className="flex gap-4">
          <Select value={locationFilter} onValueChange={setLocationFilter}>
            <SelectTrigger className="w-40">
              <MapPin className="w-4 h-4 mr-2" />
              <SelectValue placeholder={t('jobList.locationFilter')} />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="all">{t('jobList.allLocations')}</SelectItem>
              {locations.map((location) => (
                <SelectItem key={location} value={location}>
                  {location}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
          <Select value={sortBy} onValueChange={setSortBy}>
            <SelectTrigger className="w-40">
              <Filter className="w-4 h-4 mr-2" />
              <SelectValue placeholder={t('jobList.sortBy')} />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="match">{t('jobList.sortMatch')}</SelectItem>
              <SelectItem value="salary">{t('jobList.sortSalary')}</SelectItem>
              <SelectItem value="date">{t('jobList.sortDate')}</SelectItem>
            </SelectContent>
          </Select>
        </div>
      </div>

      {/* 职位列表 */}
      <div className="grid gap-4">
        {filteredJobs.length === 0 ? (
          <Card className="border-dashed">
            <CardContent className="flex flex-col items-center justify-center py-16">
              <Briefcase className="w-16 h-16 text-gray-300 mb-4" />
              <h3 className="text-lg font-medium text-gray-900 mb-2">{t('jobList.emptyTitle')}</h3>
              <p className="text-gray-500">{t('jobList.emptyDesc')}</p>
            </CardContent>
          </Card>
        ) : (
          filteredJobs.map((job) => (
            <Card key={job.jobId} className="hover:shadow-md transition-shadow">
              <CardHeader className="pb-4">
                <div className="flex items-start justify-between">
                  <div className="flex-1">
                    <div className="flex items-center space-x-3 mb-2">
                      <h3 className="text-xl font-semibold text-gray-900">{job.title}</h3>
                      {job.matchScore && (
                        <Badge
                          className={`${
                            job.matchScore >= 80
                              ? 'bg-green-100 text-green-700'
                              : job.matchScore >= 60
                              ? 'bg-blue-100 text-blue-700'
                              : 'bg-gray-100 text-gray-700'
                          }`}
                        >
                          <Star className="w-3 h-3 mr-1" />
                          {t('jobList.matchScore', { score: job.matchScore })}
                        </Badge>
                      )}
                    </div>
                    <div className="flex flex-wrap items-center gap-4 text-sm text-gray-500">
                      <span className="flex items-center">
                        <Briefcase className="w-4 h-4 mr-1" />
                        {job.company}
                      </span>
                      <span className="flex items-center">
                        <MapPin className="w-4 h-4 mr-1" />
                        {job.location}
                      </span>
                      <span className="flex items-center">
                        <DollarSign className="w-4 h-4 mr-1" />
                        {formatSalary(job.salaryMin, job.salaryMax)}
                      </span>
                      <span className="flex items-center">
                        <Calendar className="w-4 h-4 mr-1" />
                        {formatDate(job.postedAt)}
                      </span>
                    </div>
                  </div>
                  <Button variant="outline" size="sm">
                    <ExternalLink className="w-4 h-4 mr-2" />
                    {t('jobList.viewDetails')}
                  </Button>
                </div>
              </CardHeader>
              <CardContent>
                <p className="text-gray-600 mb-4">{job.description}</p>
                <div className="flex flex-wrap gap-2">
                  {job.requirements.map((req, index) => (
                    <Badge key={index} variant="secondary">
                      {req}
                    </Badge>
                  ))}
                </div>
              </CardContent>
            </Card>
          ))
        )}
      </div>
    </div>
  );
}
