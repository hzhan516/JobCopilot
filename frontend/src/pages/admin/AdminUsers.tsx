import { useEffect } from 'react';
import { useAdminStore } from '@/store/admin.store';

export default function AdminUsers() {
  const { users, userPage, userFilters, loading, fetchUsers, setUserFilters, updateUserRole, deleteUser } = useAdminStore();

  useEffect(() => { fetchUsers(); }, [userFilters.page, userFilters.size]); // eslint-disable-line react-hooks/exhaustive-deps

  const handleRoleToggle = async (id: string, currentRole: string) => {
    const newRole = currentRole === 'ADMIN' ? 'JOB_SEEKER' : 'ADMIN';
    await updateUserRole(id, newRole);
  };

  return (
    <div>
      <h1 className="text-2xl font-bold mb-4">👥 Users</h1>

      {loading ? (
        <div className="text-gray-500">Loading...</div>
      ) : (
        <>
          <div className="bg-white rounded-lg border overflow-hidden">
            <table className="w-full text-sm">
              <thead className="bg-gray-50 border-b">
                <tr>
                  <th className="text-left p-3 font-medium">Email</th>
                  <th className="text-left p-3 font-medium">Role</th>
                  <th className="text-left p-3 font-medium">Status</th>
                  <th className="text-left p-3 font-medium">Provider</th>
                  <th className="text-left p-3 font-medium">Verified</th>
                  <th className="text-right p-3 font-medium">Actions</th>
                </tr>
              </thead>
              <tbody>
                {users.map(u => (
                  <tr key={u.id} className="border-b last:border-none hover:bg-gray-50">
                    <td className="p-3">{u.email}</td>
                    <td className="p-3">
                      <span className={`px-2 py-0.5 rounded text-xs font-medium ${u.role === 'ADMIN' ? 'bg-purple-100 text-purple-800' : 'bg-gray-100 text-gray-800'}`}>
                        {u.role}
                      </span>
                    </td>
                    <td className="p-3">
                      <span className={`px-2 py-0.5 rounded text-xs font-medium ${u.status === 'ACTIVE' ? 'bg-green-100 text-green-800' : 'bg-red-100 text-red-800'}`}>
                        {u.status}
                      </span>
                    </td>
                    <td className="p-3 text-gray-500">{u.authProvider}</td>
                    <td className="p-3">{u.emailVerified ? '✅' : '❌'}</td>
                    <td className="p-3 text-right space-x-2">
                      <button
                        onClick={() => handleRoleToggle(u.id, u.role)}
                        className="text-xs px-2 py-1 rounded border hover:bg-gray-100"
                      >
                        Toggle Role
                      </button>
                      <button
                        onClick={() => deleteUser(u.id)}
                        className="text-xs px-2 py-1 rounded border border-red-300 text-red-600 hover:bg-red-50"
                      >
                        Delete
                      </button>
                    </td>
                  </tr>
                ))}
                {users.length === 0 && (
                  <tr><td colSpan={6} className="p-6 text-center text-gray-400">No users found</td></tr>
                )}
              </tbody>
            </table>
          </div>

          {/* Pagination */}
          <div className="flex items-center justify-between mt-4 text-sm text-gray-500">
            <span>{userPage.totalElements} total</span>
            <div className="space-x-2">
              <button
                disabled={userFilters.page === 0}
                onClick={() => setUserFilters({ page: userFilters.page - 1 })}
                className="px-3 py-1 rounded border disabled:opacity-30"
              >
                Prev
              </button>
              <span>Page {userFilters.page + 1} of {userPage.totalPages || 1}</span>
              <button
                disabled={userFilters.page >= userPage.totalPages - 1}
                onClick={() => setUserFilters({ page: userFilters.page + 1 })}
                className="px-3 py-1 rounded border disabled:opacity-30"
              >
                Next
              </button>
            </div>
          </div>
        </>
      )}
    </div>
  );
}
