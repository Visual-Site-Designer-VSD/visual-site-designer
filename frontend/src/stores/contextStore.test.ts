import { describe, it, expect, beforeEach } from 'vitest';
import { useContextStore } from './contextStore';
import { ContextDescriptor } from '../types/builder';

describe('contextStore', () => {
  beforeEach(() => {
    // Reset store to initial state before each test
    useContextStore.getState().clearContexts();
  });

  describe('initial state', () => {
    it('should have empty contexts', () => {
      const state = useContextStore.getState();
      expect(state.contexts).toEqual([]);
      expect(state.contextMap.size).toBe(0);
      expect(state.isLoading).toBe(false);
      expect(state.error).toBeNull();
      expect(state.initialized).toBe(false);
    });
  });

  describe('setContexts', () => {
    it('should set contexts and build contextMap', () => {
      const contexts: ContextDescriptor[] = [
        {
          contextId: 'auth',
          pluginId: 'auth-context-plugin',
          pluginVersion: '1.0.0',
          providerComponentPath: 'AuthProvider.js',
          apiEndpoints: [],
          requiredContexts: [],
        },
        {
          contextId: 'cart',
          pluginId: 'cart-context-plugin',
          pluginVersion: '1.0.0',
          providerComponentPath: 'CartProvider.js',
          apiEndpoints: [],
          requiredContexts: ['auth'],
        },
      ];

      useContextStore.getState().setContexts(contexts);

      const state = useContextStore.getState();
      expect(state.contexts).toHaveLength(2);
      expect(state.contextMap.size).toBe(2);
      expect(state.contextMap.get('auth')?.pluginId).toBe('auth-context-plugin');
      expect(state.contextMap.get('cart')?.pluginId).toBe('cart-context-plugin');
      expect(state.initialized).toBe(true);
    });

    it('should replace existing contexts', () => {
      useContextStore.getState().setContexts([
        {
          contextId: 'auth',
          pluginId: 'old-plugin',
          pluginVersion: '1.0.0',
          providerComponentPath: 'Old.js',
          apiEndpoints: [],
          requiredContexts: [],
        },
      ]);

      useContextStore.getState().setContexts([
        {
          contextId: 'cart',
          pluginId: 'cart-plugin',
          pluginVersion: '1.0.0',
          providerComponentPath: 'Cart.js',
          apiEndpoints: [],
          requiredContexts: [],
        },
      ]);

      const state = useContextStore.getState();
      expect(state.contexts).toHaveLength(1);
      expect(state.contextMap.has('auth')).toBe(false);
      expect(state.contextMap.has('cart')).toBe(true);
    });
  });

  describe('setLoading', () => {
    it('should update loading state', () => {
      useContextStore.getState().setLoading(true);
      expect(useContextStore.getState().isLoading).toBe(true);

      useContextStore.getState().setLoading(false);
      expect(useContextStore.getState().isLoading).toBe(false);
    });
  });

  describe('setError', () => {
    it('should set error message', () => {
      useContextStore.getState().setError('Something failed');
      expect(useContextStore.getState().error).toBe('Something failed');
    });

    it('should clear error with null', () => {
      useContextStore.getState().setError('Error');
      useContextStore.getState().setError(null);
      expect(useContextStore.getState().error).toBeNull();
    });
  });

  describe('getContext', () => {
    it('should return context by ID', () => {
      useContextStore.getState().setContexts([
        {
          contextId: 'auth',
          pluginId: 'auth-plugin',
          pluginVersion: '1.0.0',
          providerComponentPath: 'Auth.js',
          apiEndpoints: [],
          requiredContexts: [],
        },
      ]);

      const context = useContextStore.getState().getContext('auth');
      expect(context).toBeDefined();
      expect(context?.contextId).toBe('auth');
    });

    it('should return undefined for unknown context', () => {
      const context = useContextStore.getState().getContext('nonexistent');
      expect(context).toBeUndefined();
    });
  });

  describe('isContextAvailable', () => {
    it('should return true for registered context', () => {
      useContextStore.getState().setContexts([
        {
          contextId: 'auth',
          pluginId: 'auth-plugin',
          pluginVersion: '1.0.0',
          providerComponentPath: 'Auth.js',
          apiEndpoints: [],
          requiredContexts: [],
        },
      ]);

      expect(useContextStore.getState().isContextAvailable('auth')).toBe(true);
    });

    it('should return false for unregistered context', () => {
      expect(useContextStore.getState().isContextAvailable('unknown')).toBe(false);
    });
  });

  describe('areContextsAvailable', () => {
    beforeEach(() => {
      useContextStore.getState().setContexts([
        {
          contextId: 'auth',
          pluginId: 'auth-plugin',
          pluginVersion: '1.0.0',
          providerComponentPath: 'Auth.js',
          apiEndpoints: [],
          requiredContexts: [],
        },
        {
          contextId: 'cart',
          pluginId: 'cart-plugin',
          pluginVersion: '1.0.0',
          providerComponentPath: 'Cart.js',
          apiEndpoints: [],
          requiredContexts: [],
        },
      ]);
    });

    it('should return true when all contexts available', () => {
      expect(useContextStore.getState().areContextsAvailable(['auth', 'cart'])).toBe(true);
    });

    it('should return false when some contexts missing', () => {
      expect(useContextStore.getState().areContextsAvailable(['auth', 'order'])).toBe(false);
    });

    it('should return true for empty list', () => {
      expect(useContextStore.getState().areContextsAvailable([])).toBe(true);
    });
  });

  describe('getMissingContexts', () => {
    beforeEach(() => {
      useContextStore.getState().setContexts([
        {
          contextId: 'auth',
          pluginId: 'auth-plugin',
          pluginVersion: '1.0.0',
          providerComponentPath: 'Auth.js',
          apiEndpoints: [],
          requiredContexts: [],
        },
      ]);
    });

    it('should return missing context IDs', () => {
      const missing = useContextStore.getState().getMissingContexts(['auth', 'cart', 'order']);
      expect(missing).toEqual(['cart', 'order']);
    });

    it('should return empty when all available', () => {
      const missing = useContextStore.getState().getMissingContexts(['auth']);
      expect(missing).toEqual([]);
    });
  });

  describe('clearContexts', () => {
    it('should reset to initial state', () => {
      useContextStore.getState().setContexts([
        {
          contextId: 'auth',
          pluginId: 'auth-plugin',
          pluginVersion: '1.0.0',
          providerComponentPath: 'Auth.js',
          apiEndpoints: [],
          requiredContexts: [],
        },
      ]);

      useContextStore.getState().clearContexts();

      const state = useContextStore.getState();
      expect(state.contexts).toEqual([]);
      expect(state.contextMap.size).toBe(0);
      expect(state.initialized).toBe(false);
    });
  });
});
