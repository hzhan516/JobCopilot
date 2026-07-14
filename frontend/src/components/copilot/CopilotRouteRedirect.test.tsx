import { describe, expect, it, vi } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import CopilotRouteRedirect from './CopilotRouteRedirect';

const open = vi.hoisted(() => vi.fn());

vi.mock('@/store/copilot.store', () => ({
  useCopilotStore: (selector: (state: { open: typeof open }) => unknown) => selector({ open }),
}));

describe('CopilotRouteRedirect', () => {
  it('opens the responsive Copilot surface and redirects to the workspace', async () => {
    render(
      <MemoryRouter initialEntries={['/chat']}>
        <Routes>
          <Route path="/chat" element={<CopilotRouteRedirect />} />
          <Route path="/" element={<div data-testid="workspace" />} />
        </Routes>
      </MemoryRouter>,
    );

    expect(screen.getByTestId('workspace')).toBeInTheDocument();
    await waitFor(() => expect(open).toHaveBeenCalledWith({ type: 'general' }));
  });
});
