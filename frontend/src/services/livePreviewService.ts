import api from './api';
import { useAuthStore } from '../stores/authStore';

/** The live preview as the builder server reports it. */
export interface LivePreviewInfo {
  running: boolean;
  siteId: number | null;
  /** Where to open the running site. */
  url: string | null;
  /** The runtime console, with a one-time token in the link so nothing has to be typed. */
  consoleUrl: string | null;
  /** The server stops the preview if it hears nothing for this long. */
  idleTimeoutSeconds: number;
}

/** Message to show a designer for a failed call (the server explains why a preview could not start). */
export function livePreviewError(error: unknown): string {
  const e = error as { response?: { data?: { error?: string } }; message?: string; code?: string };
  if (e?.response?.data?.error) return e.response.data.error;
  if (e?.code === 'ECONNABORTED') return 'The live server took too long to start.';
  return e?.message ?? 'The live server could not be started.';
}

/** Runs the site being designed on the live runtime, on this machine, until the preview window is closed. */
export const livePreviewService = {
  /** Builds the site's saved pages into a snapshot and starts the runtime for it. Takes a few seconds. */
  start: async (siteId: number): Promise<LivePreviewInfo> => {
    const response = await api.post<LivePreviewInfo>(`/sites/${siteId}/live-preview`, undefined, { timeout: 90_000 });
    return response.data;
  },

  status: async (): Promise<LivePreviewInfo> => (await api.get<LivePreviewInfo>('/live-preview')).data,

  /** Tells the server the preview is still open. */
  heartbeat: async (): Promise<boolean> => (await api.post<{ running: boolean }>('/live-preview/heartbeat')).data.running,

  stop: async (): Promise<void> => {
    await api.delete('/live-preview');
  },

  /**
   * Stops the preview while the page is being closed. A normal request may be cut off then, so this one asks the
   * browser to finish it anyway. If it still fails, the server stops the preview itself after the idle timeout.
   */
  stopOnExit: (): void => {
    const token = useAuthStore.getState().accessToken;
    if (!token) return;
    try {
      void fetch('/api/live-preview', { method: 'DELETE', keepalive: true, headers: { Authorization: `Bearer ${token}` } });
    } catch {
      /* the idle timeout is the safety net */
    }
  },
};
