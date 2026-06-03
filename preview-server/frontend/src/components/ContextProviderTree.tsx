import { useState, useEffect, createContext, ReactNode } from 'react';
import { loadPlugin } from '../services/pluginLoader';

/**
 * Context registry — plugins register their context values here.
 * Mirrors the CMS's PluginContextRegistryContext pattern.
 */
export const PluginContextRegistryContext = createContext<Map<string, any>>(new Map());

interface ContextDescriptor {
  contextId: string;
  pluginId: string;
  displayName: string;
}

interface ContextProviderTreeProps {
  children: ReactNode;
}

export default function ContextProviderTree({ children }: ContextProviderTreeProps) {
  const [contextRegistry, setContextRegistry] = useState<Map<string, any>>(new Map());
  const [loaded, setLoaded] = useState(false);

  // Wire up global registration functions
  useEffect(() => {
    const vsd = (window as any).__VSD__;
    if (vsd) {
      vsd._registerFn = (contextId: string, value: any) => {
        setContextRegistry(prev => {
          const next = new Map(prev);
          next.set(contextId, value);
          return next;
        });
      };
      vsd._unregisterFn = (contextId: string) => {
        setContextRegistry(prev => {
          const next = new Map(prev);
          next.delete(contextId);
          return next;
        });
      };
    }
  }, []);

  // Fetch context descriptors and load context plugins
  useEffect(() => {
    async function loadContextPlugins() {
      try {
        const res = await fetch('/api/contexts', { credentials: 'same-origin' });
        if (!res.ok) {
          console.warn('[ContextProviderTree] No context descriptors available');
          setLoaded(true);
          return;
        }
        const descriptors: ContextDescriptor[] = await res.json();

        // Load each context plugin's bundle
        const pluginIds = [...new Set(descriptors.map(d => d.pluginId))];
        await Promise.allSettled(pluginIds.map(id => loadPlugin(id)));
      } catch (err) {
        console.warn('[ContextProviderTree] Failed to load context plugins:', err);
      } finally {
        setLoaded(true);
      }
    }

    loadContextPlugins();
  }, []);

  if (!loaded) {
    return (
      <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', minHeight: '100vh' }}>
        Loading...
      </div>
    );
  }

  return (
    <PluginContextRegistryContext.Provider value={contextRegistry}>
      {children}
    </PluginContextRegistryContext.Provider>
  );
}
