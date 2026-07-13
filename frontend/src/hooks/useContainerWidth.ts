import { useLayoutEffect, useRef, useState } from 'react';

/**
 * 测量某个元素的当前宽度（px），并在其尺寸变化时更新。
 *
 * 用于 Master-Detail 布局按“实际可用内容宽度”而非“浏览器视口宽度”决定是否分栏，
 * 从而避免在视口够宽但内容区被侧栏/内边距挤窄时错误启用双栏。
 *
 * @returns `[ref, width]`
 *   - `ref`  绑定到需要测量的元素
 *   - `width` 元素宽度（px）；首次渲染（尚未测量）时为 `null`
 */
export function useContainerWidth<T extends HTMLElement>() {
  const ref = useRef<T | null>(null);
  const [width, setWidth] = useState<number | null>(null);

  useLayoutEffect(() => {
    const el = ref.current;
    if (!el) return;

    const measure = () => setWidth(el.getBoundingClientRect().width);
    measure();

    if (typeof ResizeObserver === 'undefined') return;
    const observer = new ResizeObserver(measure);
    observer.observe(el);
    return () => observer.disconnect();
  }, []);

  return [ref, width] as const;
}
