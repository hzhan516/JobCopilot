import { describe, expect, it, vi } from 'vitest';
import { fireEvent, render, screen } from '@testing-library/react';
import CopilotRail from './CopilotRail';

const state = vi.hoisted(() => ({
  railCollapsed: false,
  toggleRail: vi.fn(),
}));

vi.mock('react-i18next', () => ({
  useTranslation: () => ({ t: (key: string) => key }),
}));

vi.mock('@/store/copilot.store', () => ({
  useCopilotStore: () => state,
}));

vi.mock('@/components/copilot/CopilotChatArea', () => ({
  default: () => <div data-testid="copilot-chat-area">Chat</div>,
}));

describe('CopilotRail', () => {
  it('renders one persistent chat area when expanded', () => {
    state.railCollapsed = false;
    render(<CopilotRail />);

    expect(screen.getByTestId('copilot-rail-region')).toHaveAttribute('data-collapsed', 'false');
    expect(screen.getByTestId('copilot-chat-area')).toBeInTheDocument();
  });

  it('collapses to an icon rail without mounting chat twice', () => {
    state.railCollapsed = true;
    render(<CopilotRail />);

    expect(screen.getByTestId('copilot-rail-region')).toHaveAttribute('data-collapsed', 'true');
    expect(screen.queryByTestId('copilot-chat-area')).not.toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: 'layout.sidebar.copilot.expand' }));
    expect(state.toggleRail).toHaveBeenCalled();
  });
});
