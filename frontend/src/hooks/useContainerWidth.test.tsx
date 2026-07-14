import { act, render, screen } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { useContainerWidth } from './useContainerWidth';

let measuredWidth = 0;
let resizeCallback: ResizeObserverCallback;
const observe = vi.fn();
const disconnect = vi.fn();

function WidthProbe() {
  const [ref, width] = useContainerWidth<HTMLDivElement>();

  return (
    <div ref={ref} data-testid="width-probe">
      {width === null ? 'unmeasured' : width}
    </div>
  );
}

describe('useContainerWidth', () => {
  beforeEach(() => {
    measuredWidth = 806;
    observe.mockClear();
    disconnect.mockClear();

    vi.spyOn(HTMLElement.prototype, 'getBoundingClientRect').mockImplementation(
      () =>
        ({
          width: measuredWidth,
          height: 600,
          top: 0,
          right: measuredWidth,
          bottom: 600,
          left: 0,
          x: 0,
          y: 0,
          toJSON: () => ({}),
        }) as DOMRect
    );

    vi.stubGlobal(
      'ResizeObserver',
      class {
        constructor(callback: ResizeObserverCallback) {
          resizeCallback = callback;
        }

        observe = observe;
        disconnect = disconnect;
      }
    );
  });

  afterEach(() => {
    vi.restoreAllMocks();
    vi.unstubAllGlobals();
  });

  it('measures the attached container during the layout effect', () => {
    render(<WidthProbe />);

    expect(screen.getByTestId('width-probe')).toHaveTextContent('806');
    expect(observe).toHaveBeenCalledWith(screen.getByTestId('width-probe'));
  });

  it('updates when ResizeObserver reports a container size change', () => {
    render(<WidthProbe />);
    measuredWidth = 620;

    act(() => {
      resizeCallback([], {} as ResizeObserver);
    });

    expect(screen.getByTestId('width-probe')).toHaveTextContent('620');
  });

  it('disconnects the observer on unmount', () => {
    const { unmount } = render(<WidthProbe />);

    unmount();

    expect(disconnect).toHaveBeenCalledOnce();
  });

  it('still performs the initial measurement when ResizeObserver is unavailable', () => {
    vi.stubGlobal('ResizeObserver', undefined);

    render(<WidthProbe />);

    expect(screen.getByTestId('width-probe')).toHaveTextContent('806');
  });
});
