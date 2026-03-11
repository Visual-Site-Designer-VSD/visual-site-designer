import React, { useEffect, useState } from 'react';
import { useContextStore } from '../../stores/contextStore';
import { contextService } from '../../services/contextService';
import {
  PluginContextRegistryContext,
  getPluginContextValues,
} from '../../hooks/usePluginContext';

interface ContextProviderTreeProps {
  children: React.ReactNode;
}

/**
 * ContextProviderTree wraps the page component tree with all active
 * context providers in dependency-resolved order.
 *
 * It fetches active context descriptors from the backend on mount,
 * loads their provider bundles, and wraps children with each provider.
 *
 * Usage:
 * ```tsx
 * <ContextProviderTree>
 *   <Page>
 *     <LoginForm />   // can use usePluginContext('auth')
 *     <CartWidget />  // can use usePluginContext('cart')
 *   </Page>
 * </ContextProviderTree>
 * ```
 */
export const ContextProviderTree: React.FC<ContextProviderTreeProps> = ({ children }) => {
  const { contexts, setContexts, setLoading, setError, initialized } = useContextStore();
  const [contextValues] = useState(() => getPluginContextValues());

  useEffect(() => {
    if (initialized) return;

    const loadContexts = async () => {
      setLoading(true);
      try {
        const descriptors = await contextService.getActiveContexts();
        setContexts(descriptors);
      } catch (err) {
        console.error('Failed to load context providers:', err);
        setError(err instanceof Error ? err.message : 'Failed to load contexts');
      } finally {
        setLoading(false);
      }
    };

    loadContexts();
  }, [initialized, setContexts, setLoading, setError]);

  // Log active contexts for debugging
  useEffect(() => {
    if (contexts.length > 0) {
      console.log(
        `[ContextProviderTree] ${contexts.length} active context(s):`,
        contexts.map(c => c.contextId)
      );
    }
  }, [contexts]);

  return (
    <PluginContextRegistryContext.Provider value={contextValues}>
      {children}
    </PluginContextRegistryContext.Provider>
  );
};
