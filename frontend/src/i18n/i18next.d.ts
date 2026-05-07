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

// Allow dynamic string keys (e.g., from backend-driven labels) beyond static JSON keys
// 允许动态字符串键（如后端驱动的标签），超出静态 JSON 键的范围
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
