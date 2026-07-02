import { create } from 'zustand';

/** Copilot Drawer 的唤起上下文 */
export type CopilotContext =
  | { type: 'general' }
  | { type: 'job'; id: string; title: string };

interface CopilotState {
  isOpen: boolean;
  context: CopilotContext | null;
  /** 以指定上下文打开 Drawer */
  open: (ctx?: CopilotContext) => void;
  /** 关闭 Drawer 并清空上下文 */
  close: () => void;
}

export const useCopilotStore = create<CopilotState>((set) => ({
  isOpen: false,
  context: null,
  open: (ctx) => set({ isOpen: true, context: ctx ?? { type: 'general' } }),
  close: () => set({ isOpen: false, context: null }),
}));
