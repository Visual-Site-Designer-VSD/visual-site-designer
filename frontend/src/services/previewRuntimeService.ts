import api from './api';

export interface PreviewStatus {
  deployed: boolean;
  url: string | null;
  siteId: number | null;
  restartRequired: boolean;
  pageCount: number;
  error: string | null;
}

/**
 * Deploy a site snapshot to the running preview server.
 */
export async function deployToPreview(siteId: number): Promise<PreviewStatus> {
  const response = await api.post<PreviewStatus>(`/preview/deploy?siteId=${siteId}`);
  return response.data;
}

/**
 * Get current deployment status from the preview server.
 */
export async function getPreviewStatus(): Promise<PreviewStatus> {
  const response = await api.get<PreviewStatus>('/preview/status');
  return response.data;
}

/**
 * Redeploy the site snapshot to the preview server.
 */
export async function refreshPreview(siteId: number): Promise<PreviewStatus> {
  const response = await api.post<PreviewStatus>(`/preview/refresh?siteId=${siteId}`);
  return response.data;
}
