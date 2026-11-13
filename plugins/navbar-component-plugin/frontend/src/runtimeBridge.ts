/**
 * Optional bridge to the VSD live runtime (window.VSD.runtime).
 * Absent in the builder and previews, where the navbar behaves as before.
 */

export interface RuntimeUser {
  id: string;
  username: string;
  displayName: string;
  email?: string;
  roles: string[];
}

interface VsdRuntime {
  useAuth(): { user: RuntimeUser | null };
}

export const getRuntime = (): VsdRuntime | undefined =>
  (globalThis as unknown as { VSD?: { runtime?: VsdRuntime } }).VSD?.runtime;

/** The signed-in visitor, or null (also null when there is no live runtime). */
export function useRuntimeUser(): RuntimeUser | null {
  const rt = getRuntime();
  return rt ? rt.useAuth().user : null;
}
