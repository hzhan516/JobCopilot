import 'i18next';
import en from '@/locales/en.json';

type Resources = typeof en;

declare module 'i18next' {
  interface CustomTypeOptions {
    resources: {
      translation: Resources;
    };
  }
}

declare module 'react-i18next' {
  interface CustomTypeOptions {
    resources: {
      translation: Resources;
    };
  }
}

// Override TFunction to accept any string key for dynamic lookups
declare module 'i18next' {
  export interface TFunction {
    (
      key: string | string[],
      options?: Record<string, unknown> | null
    ): string;
    (
      key: string | string[],
      defaultValue: string,
      options?: Record<string, unknown> | null
    ): string;
  }
}
