import { useCallback, useEffect, useState } from 'react';
import { getUserTimeZone, setUserTimeZone, clearUserTimeZone } from '@/utils/i18n';

export function useTimeZone() {
  const [timeZone, setTimeZoneState] = useState<string>(getUserTimeZone());

  useEffect(() => {
    const handleStorage = (e: StorageEvent) => {
      if (e.key === 'user-timezone') {
        setTimeZoneState(getUserTimeZone());
      }
    };
    window.addEventListener('storage', handleStorage);
    return () => window.removeEventListener('storage', handleStorage);
  }, []);

  const updateTimeZone = useCallback((tz: string) => {
    setUserTimeZone(tz);
    setTimeZoneState(tz);
  }, []);

  const resetTimeZone = useCallback(() => {
    clearUserTimeZone();
    setTimeZoneState(getUserTimeZone());
  }, []);

  return {
    timeZone,
    updateTimeZone,
    resetTimeZone,
    isAuto: timeZone === Intl.DateTimeFormat().resolvedOptions().timeZone,
  };
}
