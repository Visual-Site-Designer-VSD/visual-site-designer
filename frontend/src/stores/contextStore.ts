import { create } from 'zustand';
import { ContextDescriptor } from '../types/builder';

/**
 * Context provider store interface
 */
interface ContextState {
  /** Active context descriptors (sorted by dependency order) */
  contexts: ContextDescriptor[];

  /** Map of contextId -> ContextDescriptor for quick lookup */
  contextMap: Map<string, ContextDescriptor>;

  /** Loading and error states */
  isLoading: boolean;
  error: string | null;

  /** Whether contexts have been fetched at least once */
  initialized: boolean;

  /** Actions */
  setContexts: (contexts: ContextDescriptor[]) => void;
  setLoading: (loading: boolean) => void;
  setError: (error: string | null) => void;
  getContext: (contextId: string) => ContextDescriptor | undefined;
  isContextAvailable: (contextId: string) => boolean;
  areContextsAvailable: (contextIds: string[]) => boolean;
  getMissingContexts: (contextIds: string[]) => string[];
  clearContexts: () => void;
}

/**
 * Create context store using Zustand.
 * Stores active context provider descriptors fetched from the backend.
 */
export const useContextStore = create<ContextState>((set, get) => ({
  contexts: [],
  contextMap: new Map(),
  isLoading: false,
  error: null,
  initialized: false,

  setContexts: (contexts: ContextDescriptor[]) => {
    const contextMap = new Map<string, ContextDescriptor>();
    for (const ctx of contexts) {
      contextMap.set(ctx.contextId, ctx);
    }
    set({ contexts, contextMap, initialized: true });
  },

  setLoading: (loading: boolean) => set({ isLoading: loading }),

  setError: (error: string | null) => set({ error }),

  getContext: (contextId: string) => {
    return get().contextMap.get(contextId);
  },

  isContextAvailable: (contextId: string) => {
    return get().contextMap.has(contextId);
  },

  areContextsAvailable: (contextIds: string[]) => {
    const contextMap = get().contextMap;
    return contextIds.every(id => contextMap.has(id));
  },

  getMissingContexts: (contextIds: string[]) => {
    const contextMap = get().contextMap;
    return contextIds.filter(id => !contextMap.has(id));
  },

  clearContexts: () => {
    set({ contexts: [], contextMap: new Map(), initialized: false });
  },
}));
