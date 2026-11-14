import { useCallback, useEffect, useRef, useState } from 'react';
import { LivePreviewInfo, livePreviewError, livePreviewService } from '../services/livePreviewService';

export type LiveServerState = 'stopped' | 'starting' | 'running';

const WINDOW_NAME = 'vsd-live-preview';

const WAITING_PAGE =
  '<!doctype html><title>Live preview</title>' +
  '<body style="font-family:system-ui,sans-serif;padding:3rem;color:#333">' +
  '<h2>Starting the live server…</h2><p>This takes a few seconds. The server stops when you close this window.</p>';

/** Shows a message in the preview window (as text, never as markup). */
function showInWindow(w: Window, title: string, message: string): void {
  try {
    w.document.open();
    w.document.write('<!doctype html><title>Live preview</title><body style="font-family:system-ui,sans-serif;padding:3rem;color:#333"><h2></h2><pre style="white-space:pre-wrap"></pre>');
    w.document.close();
    w.document.querySelector('h2')!.textContent = title;
    w.document.querySelector('pre')!.textContent = message;
  } catch {
    /* the window was closed meanwhile */
  }
}

/**
 * "Run on live server": saves, asks the builder server to start the runtime for the site, and opens it in a
 * window. The server runs only while that window is open. Closing the window stops it at once; if the builder
 * itself goes away, the server stops it after a short idle time.
 */
export function useLiveServer(siteId: number | null, save: () => Promise<unknown> | void) {
  const [state, setState] = useState<LiveServerState>('stopped');
  const [info, setInfo] = useState<LivePreviewInfo | null>(null);
  const win = useRef<Window | null>(null);
  const timers = useRef<number[]>([]);
  const running = useRef(false);

  const clearTimers = () => {
    timers.current.forEach((t) => window.clearInterval(t));
    timers.current = [];
  };

  /** Back to "stopped" here; the server is told when `tell` is set. */
  const finish = useCallback(async (tell: boolean, closeWindow: boolean) => {
    clearTimers();
    running.current = false;
    if (closeWindow) {
      try {
        win.current?.close();
      } catch {
        /* already closed */
      }
    }
    win.current = null;
    setState('stopped');
    setInfo(null);
    if (tell) {
      try {
        await livePreviewService.stop();
      } catch {
        /* the server stops it by itself after the idle timeout */
      }
    }
  }, []);

  const watch = useCallback(
    (idleSeconds: number) => {
      clearTimers();
      // The window being closed is what ends the preview: notice it within a second.
      timers.current.push(
        window.setInterval(() => {
          if (!win.current || win.current.closed) void finish(true, false);
        }, 1000),
      );
      // Tell the server the builder is still here, so it does not stop a preview that is in use.
      const every = Math.max(5, Math.floor(idleSeconds / 3)) * 1000;
      timers.current.push(
        window.setInterval(() => {
          livePreviewService
            .heartbeat()
            .then((alive) => {
              if (!alive && running.current) void finish(false, false);
            })
            .catch(() => undefined);
        }, every),
      );
    },
    [finish],
  );

  const run = useCallback(() => {
    if (state === 'starting') return;
    if (state === 'running' && win.current && !win.current.closed) {
      win.current.focus();
      return;
    }
    if (siteId == null) {
      window.alert('Open a site first. The live server runs the pages of the site you are designing.');
      return;
    }
    // Open the window now, while the click still counts, so the pop-up blocker lets it through.
    const w = window.open('about:blank', WINDOW_NAME);
    if (!w) {
      window.alert('Could not open the live preview window. Please allow pop-ups for this site and try again.');
      return;
    }
    try {
      w.document.write(WAITING_PAGE);
      w.document.close();
    } catch {
      /* ignore */
    }
    win.current = w;
    setState('starting');

    void (async () => {
      try {
        await save();
        const started = await livePreviewService.start(siteId);
        if (!started.url) throw new Error('The server did not say where the live preview runs.');
        if (w.closed) {
          // The designer gave up while it was starting.
          await livePreviewService.stop().catch(() => undefined);
          setState('stopped');
          return;
        }
        w.location.href = started.url;
        running.current = true;
        setInfo(started);
        setState('running');
        watch(started.idleTimeoutSeconds);
      } catch (e) {
        showInWindow(w, 'The live server could not be started', livePreviewError(e));
        win.current = null;
        setState('stopped');
      }
    })();
  }, [state, siteId, save, watch]);

  const stop = useCallback(() => void finish(true, true), [finish]);

  const openConsole = useCallback(() => {
    if (info?.consoleUrl) window.open(info.consoleUrl, '_blank', 'noopener');
  }, [info]);

  // Leaving the builder ends the preview too.
  useEffect(() => {
    const onExit = () => {
      if (running.current) livePreviewService.stopOnExit();
    };
    window.addEventListener('pagehide', onExit);
    return () => {
      window.removeEventListener('pagehide', onExit);
      clearTimers();
      if (running.current) livePreviewService.stopOnExit();
    };
  }, []);

  return { state, info, run, stop, openConsole };
}
