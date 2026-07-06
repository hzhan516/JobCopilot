import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import ContextUsageIndicator from './ContextUsageIndicator';

// ponytail: smoke tests; i18n interpolations come through as-is in test (no provider)
describe('ContextUsageIndicator', () => {
  it('renders nothing when contextTokens is missing', () => {
    const { container } = render(
      <ContextUsageIndicator
        contextTokens={undefined}
        contextWindow={1000000}
        usageRatio={0}
      />
    );
    expect(container.textContent).toBe('');
  });

  it('renders nothing when contextTokens is 0', () => {
    const { container } = render(
      <ContextUsageIndicator
        contextTokens={0}
        contextWindow={1000000}
        usageRatio={0}
      />
    );
    expect(container.textContent).toBe('');
  });

  it('renders nothing when contextWindow is 0', () => {
    const { container } = render(
      <ContextUsageIndicator
        contextTokens={100}
        contextWindow={0}
        usageRatio={0}
      />
    );
    expect(container.textContent).toBe('');
  });

  it('renders non-empty content when data is available', () => {
    const { container } = render(
      <ContextUsageIndicator
        contextTokens={199300}
        contextWindow={1000000}
        usageRatio={0.1993}
        compactAdvised={false}
      />
    );
    // i18n interpolation renders raw tokens in test; check it's non-empty
    expect(container.textContent).not.toBe('');
  });

  it('shows compact button when advised', () => {
    const onCompact = vi.fn();
    render(
      <ContextUsageIndicator
        contextTokens={900000}
        contextWindow={1000000}
        usageRatio={0.9}
        compactAdvised={true}
        onCompact={onCompact}
      />
    );
    const button = screen.getByRole('button');
    expect(button).toBeDefined();
  });

  it('disables compact button when compacting', () => {
    const onCompact = vi.fn();
    render(
      <ContextUsageIndicator
        contextTokens={900000}
        contextWindow={1000000}
        usageRatio={0.9}
        compactAdvised={true}
        compacting={true}
        onCompact={onCompact}
      />
    );
    const button = screen.getByRole('button');
    expect(button).toBeDisabled();
  });
});
