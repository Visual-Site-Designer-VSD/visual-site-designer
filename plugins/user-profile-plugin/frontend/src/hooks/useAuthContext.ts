import { useContext } from 'react';

/**
 * Auth context value shape provided by the auth-context-plugin.
 */
export interface AuthContextValue {
  isAuthenticated: boolean;
  user: {
    id: string;
    email: string;
    name: string;
    avatarUrl?: string;
  } | null;
  isLoading: boolean;
  error: string | null;
  login: (providerId?: string) => void;
  logout: () => Promise<void>;
  clearError: () => void;
}

/**
 * Hook to access the auth context provided by auth-context-plugin.
 * Returns undefined if auth-context-plugin is not installed.
 *
 * This hook accesses the VSD plugin context system via the global
 * window.__VSD__ API exposed by the main frontend.
 */
export function useAuthContext(): AuthContextValue | undefined {
  const vsd = (window as unknown as Record<string, unknown>).__VSD__ as
    | { PluginContextRegistryContext?: React.Context<Map<string, unknown>> }
    | undefined;

  // If VSD context system is not available, return undefined
  if (!vsd?.PluginContextRegistryContext) return undefined;

  try {
    // eslint-disable-next-line react-hooks/rules-of-hooks
    const registry = useContext(vsd.PluginContextRegistryContext);
    return registry?.get('auth') as AuthContextValue | undefined;
  } catch {
    return undefined;
  }
}
