import { useState, useEffect } from 'react';
import { adminService } from '@/services/adminService';

// localStorage key for dismissal; same version won't re-show
const DISMISS_KEY = 'admin_version_banner_dismissed';

/** 版本更新日志（前端可感知的变更） */
const CHANGELOG: Record<string, string[]> = {
  '1.1.0-pr1-ui-redesign': [
    'New side-navigation layout replacing top header',
    'Global AI Copilot drawer accessible from any page',
    'Master-detail split views for Jobs, Resumes, and Tracking',
    'Collapsible sidebar with icon-only mode',
    'Keyboard shortcuts: Cmd/Ctrl+. for Copilot, Cmd/Ctrl+B for sidebar',
  ],
};

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

  const changes = CHANGELOG[version];

  return (
    <div className="bg-blue-50 border border-blue-200 text-blue-800 px-4 py-3 rounded mb-4 text-sm">
      <div className="flex items-center justify-between">
        <span>
          🆕 <strong>Updated to {version}</strong>
        </span>
        <button
          onClick={() => { localStorage.setItem(DISMISS_KEY, version); setVisible(false); }}
          className="text-blue-500 hover:text-blue-700 font-bold ml-4 shrink-0"
          aria-label="Dismiss"
        >
          ✕
        </button>
      </div>

      {changes && changes.length > 0 && (
        <ul className="mt-2 ml-5 list-disc text-blue-700 space-y-0.5">
          {changes.map((item, i) => (
            <li key={i}>{item}</li>
          ))}
        </ul>
      )}
    </div>
  );
}
