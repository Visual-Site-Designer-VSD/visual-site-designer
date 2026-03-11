import api from './api';
import { ContextDescriptor } from '../types/builder';

/**
 * Context Registry API Service
 * Handles all API calls related to the context provider registry.
 */
export const contextService = {
  /**
   * Get all active context descriptors (sorted by dependency order).
   */
  getActiveContexts: async (): Promise<ContextDescriptor[]> => {
    const response = await api.get<ContextDescriptor[]>('/contexts');
    return response.data;
  },

  /**
   * Get a specific context descriptor by context ID.
   */
  getContext: async (contextId: string): Promise<ContextDescriptor> => {
    const response = await api.get<ContextDescriptor>(`/contexts/${contextId}`);
    return response.data;
  },

  /**
   * Validate that a list of required contexts are all available.
   * Returns list of missing context IDs (empty if all present).
   */
  validateRequiredContexts: async (requiredContexts: string[]): Promise<string[]> => {
    const response = await api.post<string[]>('/contexts/validate', requiredContexts);
    return response.data;
  },
};
