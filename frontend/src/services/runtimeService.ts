import api from './api';

/** One field of an object-shaped shared state. */
export interface SharedStateField {
  type: string;
  required: boolean;
  maxLength?: number;
}

/** A shared state as declared in the live runtime's console. */
export interface SharedStateDefinition {
  name: string;
  description?: string;
  kind: 'OBJECT' | 'MAP';
  lifetime: 'VISITOR' | 'LOGIN';
  /** OBJECT: the fields. */
  fields?: Record<string, SharedStateField>;
  /** MAP: type of every value. */
  valueType?: string;
  writers: string[];
  clientWritable: boolean;
  defaultValue?: unknown;
}

/** What the server knows about the runtime link. */
export interface RuntimeCatalog {
  /** A runtime URL is set in the server configuration. */
  configured: boolean;
  /** The runtime answered. */
  connected: boolean;
  message?: string | null;
  /** Where designers declare shared state. */
  consoleUrl: string;
  strict: boolean;
  /** Where the declarations were read from: the running runtime, the runtime's file, or nowhere. */
  source?: 'runtime' | 'file' | 'none';
  definitions: SharedStateDefinition[];
}

/** Talks to the builder server, which asks the live runtime. The runtime's admin token never reaches the browser. */
export const runtimeService = {
  getSharedState: async (): Promise<RuntimeCatalog> => {
    const response = await api.get<RuntimeCatalog>('/runtime/shared-state');
    return response.data;
  },
};
