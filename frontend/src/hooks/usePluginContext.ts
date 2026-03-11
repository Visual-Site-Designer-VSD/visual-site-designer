import { useContext, createContext } from 'react';

/**
 * Registry of plugin context values.
 * Context provider plugins register their values here at runtime.
 */
const pluginContextValues = new Map<string, any>();

/**
 * React context that carries all plugin context values.
 * The ContextProviderTree component provides this context.
 */
export const PluginContextRegistryContext = createContext<Map<string, any>>(pluginContextValues);

/**
 * Hook to consume a registered plugin context by context ID.
 *
 * Usage in a UI component plugin:
 * ```tsx
 * function LoginForm(props) {
 *   const { user, login, logout, isAuthenticated } = usePluginContext('auth');
 *   // ...
 * }
 * ```
 *
 * @param contextId The context identifier (e.g., "auth", "cart")
 * @returns The context value, or undefined if the context is not available
 */
export function usePluginContext<T = any>(contextId: string): T | undefined {
  const registry = useContext(PluginContextRegistryContext);
  return registry.get(contextId) as T | undefined;
}

/**
 * Register a plugin context value at runtime.
 * Called by context provider plugin bundles when they initialize.
 *
 * @param contextId The context identifier
 * @param value The context value (state, actions, etc.)
 */
export function registerPluginContextValue(contextId: string, value: any): void {
  pluginContextValues.set(contextId, value);
}

/**
 * Unregister a plugin context value.
 * Called when a context provider plugin is deactivated.
 *
 * @param contextId The context identifier to remove
 */
export function unregisterPluginContextValue(contextId: string): void {
  pluginContextValues.delete(contextId);
}

/**
 * Get all registered plugin context values.
 * Used by ContextProviderTree to provide all values.
 */
export function getPluginContextValues(): Map<string, any> {
  return pluginContextValues;
}
