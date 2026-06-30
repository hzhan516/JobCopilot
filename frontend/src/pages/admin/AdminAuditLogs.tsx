import { useEffect, useState } from 'react';
import { adminService, type AuditLog } from '@/services/adminService';

export default function AdminAuditLogs() {
  const [logs, setLogs] = useState<AuditLog[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    adminService.listAuditLogs({ page: 0, size: 50 }).then(r => {
      setLogs(r.content); setLoading(false);
    }).catch(() => setLoading(false));
  }, []);

  return (
    <div>
      <h1 className="text-2xl font-bold mb-4">📋 Audit Logs</h1>
      {loading ? <div className="text-gray-500">Loading...</div> : (
        <div className="bg-white rounded-lg border">
          <table className="w-full text-sm">
            <thead className="bg-gray-50 border-b">
              <tr>
                <th className="text-left p-3 font-medium">Action</th>
                <th className="text-left p-3 font-medium">Target</th>
                <th className="text-left p-3 font-medium">Admin</th>
                <th className="text-left p-3 font-medium">Time</th>
                <th className="text-left p-3 font-medium">Details</th>
              </tr>
            </thead>
            <tbody>
              {logs.map(l => (
                <tr key={l.id} className="border-b last:border-none hover:bg-gray-50">
                  <td className="p-3 font-mono text-xs">{l.action}</td>
                  <td className="p-3 text-xs">{l.targetType}/{l.targetId?.slice(0, 8)}...</td>
                  <td className="p-3 text-xs font-mono">{l.adminUserId.slice(0, 8)}...</td>
                  <td className="p-3 text-xs">{new Date(l.createdAt).toLocaleString()}</td>
                  <td className="p-3 text-xs text-gray-500">{l.details || '-'}</td>
                </tr>
              ))}
              {logs.length === 0 && (
                <tr><td colSpan={5} className="p-6 text-center text-gray-400">No audit logs</td></tr>
              )}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}
