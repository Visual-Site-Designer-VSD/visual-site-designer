/**
 * Optional bridge to the VSD live runtime (window.VSD.runtime).
 *
 * In the live runtime the session lives on the server, so signing in/out goes through the
 * runtime and every plugin on the page sees the change at once. In the builder and in
 * previews window.VSD is absent; the renderers then keep their original behaviour.
 */

export interface RuntimeUser {
  id: string;
  username: string;
  displayName: string;
  email?: string;
  roles: string[];
}

interface Outcome {
  ok: boolean;
  message?: string;
}

interface VsdRuntime {
  useAuth(): {
    user: RuntimeUser | null;
    login(username: string, password: string): Promise<Outcome>;
    logout(): Promise<Outcome>;
  };
}

export const getRuntime = (): VsdRuntime | undefined =>
  (globalThis as unknown as { VSD?: { runtime?: VsdRuntime } }).VSD?.runtime;

const OFFLINE = {
  user: null as RuntimeUser | null,
  login: null as null | ((u: string, p: string) => Promise<Outcome>),
  logout: null as null | (() => Promise<Outcome>),
};

/**
 * Session identity from the live runtime, or an inert value elsewhere. The runtime is either
 * present for the whole page lifetime or not at all, so the hook order never changes.
 */
export function useRuntimeAuth() {
  const rt = getRuntime();
  return rt ? rt.useAuth() : OFFLINE;
}
