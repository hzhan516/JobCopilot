import { useEffect, useState } from 'react';
import type { Job } from '@/types';
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
    title: '高级前端工程师',
    company: '字节跳动',
    location: '北京',
    description: '负责公司核心产品的前端开发工作，使用 React、TypeScript 等技术栈。',
    requirements: ['3年以上前端开发经验', '精通 React 和 TypeScript', '有大型项目经验'],
    salaryMin: 25000,
    salaryMax: 45000,
    postedAt: '2024-01-15T10:00:00',
    matchScore: 92,
  },
  {
    jobId: '2',
    title: 'Java后端开发',
    company: '阿里巴巴',
    location: '杭州',
    description: '参与电商平台的后端开发，使用 Spring Boot、MySQL、Redis 等技术。',
    requirements: ['2年以上 Java 开发经验', '熟悉 Spring Boot', '了解微服务架构'],
    salaryMin: 20000,
    salaryMax: 35000,
    postedAt: '2024-01-14T14:30:00',
    matchScore: 85,
  },
  {
    jobId: '3',
    title: '产品经理',
    company: '腾讯',
    location: '深圳',
    description: '负责社交产品的规划和设计，协调技术、设计、运营等团队。',
    requirements: ['3年以上产品经验', '有社交产品经验优先', '良好的沟通能力'],
    salaryMin: 25000,
    salaryMax: 40000,
    postedAt: '2024-01-13T09:00:00',
    matchScore: 78,
  },
  {
    jobId: '4',
    title: '数据分析师',
    company: '美团',
    location: '北京',
    description: '负责业务数据分析，为产品决策提供数据支持。',
    requirements: ['熟练使用 SQL', '掌握 Python 或 R', '有数据分析经验'],
    salaryMin: 18000,
    salaryMax: 30000,
    postedAt: '2024-01-12T16:00:00',
    matchScore: 72,
  },
  {
    jobId: '5',
    title: 'DevOps工程师',
    company: '华为',
    location: '深圳',
    description: '负责 CI/CD 流程建设，容器化部署，云平台运维。',
    requirements: ['熟悉 Docker、Kubernetes', '有云平台经验', '熟悉 Linux'],
    salaryMin: 22000,
    salaryMax: 38000,
    postedAt: '2024-01-11T11:00:00',
    matchScore: 68,
  },
];

export default function JobList() {
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
    } catch (error) {
      toast.error('加载职位列表失败');
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
    if (!min && !max) return '薪资面议';
    const format = (n: number) => (n / 1000).toFixed(0) + 'K';
    if (min && max) return `${format(min)}-${format(max)}`;
    if (min) return `${format(min)}起`;
    if (max) return `最高${format(max)}`;
    return '薪资面议';
  };

  // 渲染骨架屏
  if (isLoading) {
    return (
      <div className="space-y-6">
        <div>
          <h1 className="text-3xl font-bold text-gray-900">职位推荐</h1>
          <p className="text-gray-500 mt-1">基于您的简历智能匹配</p>
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
        <h1 className="text-3xl font-bold text-gray-900">职位推荐</h1>
        <p className="text-gray-500 mt-1">基于您的简历智能匹配</p>
      </div>

      {/* 筛选栏 */}
      <div className="flex flex-col lg:flex-row gap-4">
        <div className="relative flex-1">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" />
          <Input
            placeholder="搜索职位、公司..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            className="pl-10"
          />
        </div>
        <div className="flex gap-4">
          <Select value={locationFilter} onValueChange={setLocationFilter}>
            <SelectTrigger className="w-40">
              <MapPin className="w-4 h-4 mr-2" />
              <SelectValue placeholder="工作地点" />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="all">全部地点</SelectItem>
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
              <SelectValue placeholder="排序方式" />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="match">匹配度</SelectItem>
              <SelectItem value="salary">薪资</SelectItem>
              <SelectItem value="date">发布时间</SelectItem>
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
              <h3 className="text-lg font-medium text-gray-900 mb-2">暂无匹配的职位</h3>
              <p className="text-gray-500">请尝试调整筛选条件或更新您的简历</p>
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
                          匹配度 {job.matchScore}%
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
                        {new Date(job.postedAt).toLocaleDateString('zh-CN')}
                      </span>
                    </div>
                  </div>
                  <Button variant="outline" size="sm">
                    <ExternalLink className="w-4 h-4 mr-2" />
                    查看详情
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
