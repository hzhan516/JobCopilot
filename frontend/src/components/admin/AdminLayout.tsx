import { NavLink, Outlet } from 'react-router-dom';
import { useAuth } from '@/hooks/useAuth';
import { Users, LayoutDashboard, Activity, Settings, FileText, Bot } from 'lucide-react';

const navItems = [
  { to: '/admin', icon: LayoutDashboard, label: 'Dashboard' },
  { to: '/admin/users', icon: Users, label: 'Users' },
  { to: '/admin/audit-logs', icon: FileText, label: 'Audit Logs' },
  { to: '/admin/monitoring', icon: Activity, label: 'Monitoring' },
  { to: '/admin/config', icon: Settings, label: 'Config' },
  { to: '/admin/ai', icon: Bot, label: 'AI Service' },
];

export default function AdminLayout() {
  const { user, logout } = useAuth();

  return (
    <div className="flex h-screen bg-gray-100">
      {/* Sidebar */}
      <aside className="w-60 bg-gray-900 text-white flex flex-col shrink-0">
        <div className="p-4 text-lg font-bold border-b border-gray-700">
          ⚙️ Admin
        </div>
        <nav className="flex-1 p-2 space-y-1">
          {navItems.map(({ to, icon: Icon, label }) => (
            <NavLink
              key={to}
              to={to}
              end={to === '/admin'}
              className={({ isActive }) =>
                `flex items-center gap-3 px-3 py-2 rounded text-sm transition-colors ${
                  isActive ? 'bg-gray-700 text-white' : 'text-gray-300 hover:bg-gray-800'
                }`
              }
            >
              <Icon className="w-4 h-4" />
              {label}
            </NavLink>
          ))}
        </nav>
        <div className="p-3 border-t border-gray-700 text-xs text-gray-400 space-y-1">
          <div>{user?.email}</div>
          <button onClick={logout} className="text-blue-400 hover:underline">Logout</button>
        </div>
      </aside>

      {/* Main */}
      <main className="flex-1 overflow-auto p-6">
        <Outlet />
      </main>
    </div>
  );
}
