import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import React from 'react';
import { ContextProviderTree } from './ContextProviderTree';
import { useContextStore } from '../../stores/contextStore';

// Mock the contextService
vi.mock('../../services/contextService', () => ({
  contextService: {
    getActiveContexts: vi.fn(),
  },
}));

import { contextService } from '../../services/contextService';

describe('ContextProviderTree', () => {
  beforeEach(() => {
    // Reset store
    useContextStore.getState().clearContexts();
    vi.clearAllMocks();
  });

  it('should render children', () => {
    // Mock initialized state so it doesn't fetch
    useContextStore.setState({ initialized: true });

    render(
      <ContextProviderTree>
        <div data-testid="child">Hello</div>
      </ContextProviderTree>
    );

    expect(screen.getByTestId('child')).toBeInTheDocument();
    expect(screen.getByText('Hello')).toBeInTheDocument();
  });

  it('should fetch contexts on mount when not initialized', async () => {
    const mockContexts = [
      {
        contextId: 'auth',
        pluginId: 'auth-plugin',
        pluginVersion: '1.0.0',
        providerComponentPath: 'AuthProvider.js',
        apiEndpoints: [],
        requiredContexts: [],
      },
    ];

    (contextService.getActiveContexts as ReturnType<typeof vi.fn>).mockResolvedValue(mockContexts);

    render(
      <ContextProviderTree>
        <div>Content</div>
      </ContextProviderTree>
    );

    await waitFor(() => {
      expect(contextService.getActiveContexts).toHaveBeenCalledTimes(1);
    });

    // Verify contexts were stored
    const state = useContextStore.getState();
    expect(state.contexts).toHaveLength(1);
    expect(state.initialized).toBe(true);
  });

  it('should not fetch contexts when already initialized', () => {
    useContextStore.setState({ initialized: true });

    render(
      <ContextProviderTree>
        <div>Content</div>
      </ContextProviderTree>
    );

    expect(contextService.getActiveContexts).not.toHaveBeenCalled();
  });

  it('should set error state when fetch fails', async () => {
    (contextService.getActiveContexts as ReturnType<typeof vi.fn>).mockRejectedValue(
      new Error('Network error')
    );

    render(
      <ContextProviderTree>
        <div>Content</div>
      </ContextProviderTree>
    );

    await waitFor(() => {
      const state = useContextStore.getState();
      expect(state.error).toBe('Network error');
    });
  });

  it('should still render children when fetch fails', async () => {
    (contextService.getActiveContexts as ReturnType<typeof vi.fn>).mockRejectedValue(
      new Error('Network error')
    );

    render(
      <ContextProviderTree>
        <div data-testid="child">Still visible</div>
      </ContextProviderTree>
    );

    await waitFor(() => {
      expect(useContextStore.getState().error).toBe('Network error');
    });

    expect(screen.getByTestId('child')).toBeInTheDocument();
  });
});
