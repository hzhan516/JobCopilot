import { useEffect } from 'react';
import { Users, FileText, Briefcase, MessageSquare, Activity, Server } from 'lucide-react';
import { useAdminStore } from '@/store/admin.store';
import StatCard from '@/components/admin/StatCard';
import VersionBanner from '@/components/admin/VersionBanner';

export default function AdminDashboard() {
  const { stats, health, statsLoading, fetchStats } = useAdminStore();

  useEffect(() => { fetchStats(); }, [fetchStats]);

  return (
    <div>
      <h1 className="text-2xl font-bold mb-4">📊 Dashboard</h1>
      <VersionBanner />

      {statsLoading ? (
        <div className="text-gray-500">Loading...</div>
      ) : (
        <>
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-4 mb-6">
            <StatCard title="Users" value={stats?.userCount ?? '-'} icon={<Users className="w-5 h-5" />} />
            <StatCard title="Resumes" value={stats?.resumeCount ?? '-'} icon={<FileText className="w-5 h-5" />} />
            <StatCard title="Jobs" value={stats?.jobCount ?? '-'} icon={<Briefcase className="w-5 h-5" />} />
            <StatCard title="AI Calls" value={stats?.aiCallCount ?? '-'} icon={<MessageSquare className="w-5 h-5" />} />
            <StatCard title="Applications" value={stats?.applicationCount ?? '-'} icon={<Activity className="w-5 h-5" />} />
            <StatCard title="Conversations" value={stats?.conversationCount ?? '-'} icon={<Server className="w-5 h-5" />} />
          </div>

          <h2 className="text-lg font-semibold mb-3">🫀 System Health</h2>
          <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-5 gap-3">
            {health && Object.entries(health).map(([name, ok]) => (
              <div key={name} className={`rounded px-3 py-2 text-sm font-medium border ${ok ? 'bg-green-50 border-green-300 text-green-800' : 'bg-red-50 border-red-300 text-red-800'}`}>
                {ok ? '✅' : '❌'} {name}
              </div>
            ))}
          </div>
        </>
      )}
    </div>
  );
}
