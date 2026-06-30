import { useState, useEffect } from 'react';
import { adminService } from '@/services/adminService';

// ponytail: localStorage key for dismissal; same version won't re-show
const DISMISS_KEY = 'admin_version_banner_dismissed';

export default function VersionBanner() {
  const [visible, setVisible] = useState(false);
  const [version, setVersion] = useState('');

  useEffect(() => {
    const dismissed = localStorage.getItem(DISMISS_KEY);
    adminService.getVersion().then(v => {
      if (v.version !== dismissed) {
        setVersion(v.version);
        setVisible(true);
      }
    }).catch(() => {});
  }, []);

  if (!visible) return null;

  return (
    <div className="bg-blue-50 border border-blue-200 text-blue-800 px-4 py-2 rounded mb-4 flex items-center justify-between text-sm">
      <span>🆕 Running version: <strong>{version}</strong></span>
      <button
        onClick={() => { localStorage.setItem(DISMISS_KEY, version); setVisible(false); }}
        className="text-blue-500 hover:text-blue-700 font-bold ml-4"
      >
        ✕
      </button>
    </div>
  );
}
