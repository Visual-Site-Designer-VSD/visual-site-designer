import api from './api';

/**
 * Public API Pattern interface
 */
export interface PublicApiPattern {
  id: number;
  pattern: string;
  httpMethods: string;
  description?: string;
  enabled: boolean;
  createdBy?: number;
  createdAt: string;
  updatedAt: string;
}

/**
 * Request payload for creating a new pattern
 */
export interface CreatePatternRequest {
  pattern: string;
  httpMethods?: string;
  description?: string;
  enabled?: boolean;
}

/**
 * Request payload for updating a pattern
 */
export interface UpdatePatternRequest {
  pattern: string;
  httpMethods?: string;
  description?: string;
  enabled?: boolean;
}

/**
 * Test path request
 */
export interface TestPathRequest {
  path: string;
  method: string;
}

/**
 * Test path response
 */
export interface TestPathResponse {
  path: string;
  method: string;
  isPublic: boolean;
}

const BASE_PATH = '/admin/security/public-patterns';

/**
 * Security service for managing public API patterns
 */
export const securityService = {
  /**
   * Get all patterns (including disabled)
   */
  async getAllPatterns(): Promise<PublicApiPattern[]> {
    const response = await api.get<PublicApiPattern[]>(BASE_PATH);
    return response.data;
  },

  /**
   * Get only enabled patterns
   */
  async getEnabledPatterns(): Promise<PublicApiPattern[]> {
    const response = await api.get<PublicApiPattern[]>(`${BASE_PATH}/enabled`);
    return response.data;
  },

  /**
   * Get a pattern by ID
   */
  async getPatternById(id: number): Promise<PublicApiPattern> {
    const response = await api.get<PublicApiPattern>(`${BASE_PATH}/${id}`);
    return response.data;
  },

  /**
   * Create a new pattern
   */
  async createPattern(request: CreatePatternRequest): Promise<PublicApiPattern> {
    const response = await api.post<PublicApiPattern>(BASE_PATH, request);
    return response.data;
  },

  /**
   * Update an existing pattern
   */
  async updatePattern(id: number, request: UpdatePatternRequest): Promise<PublicApiPattern> {
    const response = await api.put<PublicApiPattern>(`${BASE_PATH}/${id}`, request);
    return response.data;
  },

  /**
   * Delete a pattern
   */
  async deletePattern(id: number): Promise<void> {
    await api.delete(`${BASE_PATH}/${id}`);
  },

  /**
   * Enable or disable a pattern
   */
  async setEnabled(id: number, enabled: boolean): Promise<PublicApiPattern> {
    const response = await api.patch<PublicApiPattern>(`${BASE_PATH}/${id}/enabled`, { enabled });
    return response.data;
  },

  /**
   * Clear the cache (force reload from database)
   */
  async clearCache(): Promise<{ message: string }> {
    const response = await api.post<{ message: string }>(`${BASE_PATH}/clear-cache`);
    return response.data;
  },

  /**
   * Test if a path would be considered public
   */
  async testPath(request: TestPathRequest): Promise<TestPathResponse> {
    const response = await api.post<TestPathResponse>(`${BASE_PATH}/test`, request);
    return response.data;
  },
};

export default securityService;
