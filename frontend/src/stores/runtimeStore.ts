import { create } from 'zustand';
import { RuntimeCatalog, runtimeService } from '../services/runtimeService';

interface RuntimeState {
  catalog: RuntimeCatalog | null;
  loading: boolean;
  /** Set when the builder server itself could not be asked (not the same as the runtime being down). */
  error: string | null;
  /** Reads the declared shared state. Several callers at once share one request. */
  load: (force?: boolean) => Promise<void>;
}

const FRESH_MS = 5000;
let inFlight: Promise<void> | null = null;
let loadedAt = 0;

export const useRuntimeStore = create<RuntimeState>((set, get) => ({
  catalog: null,
  loading: false,
  error: null,
  load: async (force = false) => {
    if (inFlight) return inFlight;
    if (!force && get().catalog && Date.now() - loadedAt < FRESH_MS) return;
    set({ loading: true });
    inFlight = runtimeService
      .getSharedState()
      .then((catalog) => {
        loadedAt = Date.now();
        set({ catalog, error: null, loading: false });
      })
      .catch((e: unknown) => {
        set({ error: e instanceof Error ? e.message : 'Could not read the runtime link', loading: false });
      })
      .finally(() => {
        inFlight = null;
      });
    return inFlight;
  },
}));
