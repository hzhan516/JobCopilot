import { create } from 'zustand';
import i18n from '@/i18n';

interface LanguageState {
  lng: string;
  setLng: (lng: string) => void;
}

export const useLanguageStore = create<LanguageState>((set) => ({
  lng: i18n.language || 'en',
  setLng: (lng: string) => {
    i18n.changeLanguage(lng);
    set({ lng });
  },
}));
