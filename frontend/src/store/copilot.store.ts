import { create } from 'zustand';

/** Copilot Drawer 的唤起上下文 */
export type CopilotContext =
  | { type: 'general' }
  | { type: 'job'; id: string; title: string };

interface CopilotState {
  isOpen: boolean;
  /** 桌面常驻 Rail 是否收起；与移动端 Drawer 开关相互独立。 */
  railCollapsed: boolean;
  context: CopilotContext | null;
  /** 以指定上下文打开 Copilot；桌面展开 Rail，窄屏打开 Drawer。 */
  open: (ctx?: CopilotContext) => void;
  /** 关闭 Drawer 并清空上下文 */
  close: () => void;
  toggleRail: () => void;
}

export const useCopilotStore = create<CopilotState>((set) => ({
  isOpen: false,
  railCollapsed: false,
  context: null,
  open: (ctx) => set({
    isOpen: true,
    railCollapsed: false,
    context: ctx ?? { type: 'general' },
  }),
  close: () => set({ isOpen: false, context: null }),
  toggleRail: () => set((state) => ({ railCollapsed: !state.railCollapsed })),
}));
