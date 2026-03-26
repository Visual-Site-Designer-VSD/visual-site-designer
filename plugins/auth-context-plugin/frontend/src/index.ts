/**
 * Auth Context Provider Plugin - Frontend Bundle
 *
 * This plugin provides shared authentication state that UI component
 * plugins can consume via usePluginContext('auth').
 *
 * Example usage in a UI component plugin:
 * ```tsx
 * const auth = usePluginContext<AuthContextValue>('auth');
 * if (auth?.isAuthenticated) {
 *   return <span>Welcome, {auth.user?.name}</span>;
 * }
 * ```
 */

export { AuthProvider } from './AuthProvider';
export type { AuthContextValue, AuthUser } from './types';

export const PLUGIN_ID = 'auth-context-plugin';
export const CONTEXT_ID = 'auth';
