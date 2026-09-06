import { useEffect, useRef } from 'react';
import { logInteraction } from './api';

/** Time-in-viewport dwell tracking. Fires one `dwell` event after >=3s visible. */
export function useDwell(itemId: number, enabled: boolean) {
  const ref = useRef<HTMLDivElement>(null);
  const visibleSince = useRef<number | null>(null);
  const fired = useRef(false);

  useEffect(() => {
    if (!enabled) return;
    fired.current = false;
    const el = ref.current;
    if (!el) return;

    const observer = new IntersectionObserver(
      (entries) => {
        const e = entries[0];
        if (e.isIntersecting) {
          visibleSince.current = Date.now();
        } else if (visibleSince.current !== null && !fired.current) {
          const ms = Date.now() - visibleSince.current;
          visibleSince.current = null;
          if (ms >= 3000) {
            fired.current = true;
            logInteraction(itemId, 'dwell', ms).catch(() => {});
          }
        }
      },
      { threshold: 0.5 }
    );
    observer.observe(el);
    const onHide = () => {
      if (visibleSince.current !== null && !fired.current) {
        const ms = Date.now() - visibleSince.current;
        if (ms >= 3000) {
          fired.current = true;
          logInteraction(itemId, 'dwell', ms).catch(() => {});
        }
        visibleSince.current = null;
      }
    };
    window.addEventListener('beforeunload', onHide);
    return () => {
      onHide();
      observer.disconnect();
      window.removeEventListener('beforeunload', onHide);
    };
  }, [itemId, enabled]);

  return ref;
}
