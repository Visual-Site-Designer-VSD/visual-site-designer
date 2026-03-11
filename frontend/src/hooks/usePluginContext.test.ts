import { describe, it, expect, beforeEach } from 'vitest';
import { renderHook } from '@testing-library/react';
import React from 'react';
import {
  usePluginContext,
  registerPluginContextValue,
  unregisterPluginContextValue,
  getPluginContextValues,
  PluginContextRegistryContext,
} from './usePluginContext';

describe('usePluginContext', () => {
  beforeEach(() => {
    // Clear all registered contexts before each test
    const values = getPluginContextValues();
    values.clear();
  });

  describe('registerPluginContextValue', () => {
    it('should register a context value', () => {
      const authValue = { user: null, login: () => {}, logout: () => {} };
      registerPluginContextValue('auth', authValue);

      const values = getPluginContextValues();
      expect(values.get('auth')).toBe(authValue);
    });

    it('should overwrite an existing context value', () => {
      registerPluginContextValue('auth', { version: 1 });
      registerPluginContextValue('auth', { version: 2 });

      const values = getPluginContextValues();
      expect(values.get('auth')).toEqual({ version: 2 });
    });
  });

  describe('unregisterPluginContextValue', () => {
    it('should remove a registered context value', () => {
      registerPluginContextValue('auth', { user: null });
      unregisterPluginContextValue('auth');

      const values = getPluginContextValues();
      expect(values.has('auth')).toBe(false);
    });

    it('should not throw when unregistering a non-existent value', () => {
      expect(() => unregisterPluginContextValue('nonexistent')).not.toThrow();
    });
  });

  describe('getPluginContextValues', () => {
    it('should return the shared Map instance', () => {
      const values1 = getPluginContextValues();
      const values2 = getPluginContextValues();
      expect(values1).toBe(values2);
    });
  });

  describe('usePluginContext hook', () => {
    it('should return registered context value', () => {
      const authValue = { user: 'testuser', isAuthenticated: true };
      registerPluginContextValue('auth', authValue);

      const wrapper = ({ children }: { children: React.ReactNode }) =>
        React.createElement(
          PluginContextRegistryContext.Provider,
          { value: getPluginContextValues() },
          children
        );

      const { result } = renderHook(() => usePluginContext('auth'), { wrapper });
      expect(result.current).toBe(authValue);
    });

    it('should return undefined for unregistered context', () => {
      const wrapper = ({ children }: { children: React.ReactNode }) =>
        React.createElement(
          PluginContextRegistryContext.Provider,
          { value: getPluginContextValues() },
          children
        );

      const { result } = renderHook(() => usePluginContext('nonexistent'), { wrapper });
      expect(result.current).toBeUndefined();
    });

    it('should return typed value', () => {
      interface AuthContext {
        user: string;
        isAuthenticated: boolean;
      }

      registerPluginContextValue('auth', { user: 'admin', isAuthenticated: true });

      const wrapper = ({ children }: { children: React.ReactNode }) =>
        React.createElement(
          PluginContextRegistryContext.Provider,
          { value: getPluginContextValues() },
          children
        );

      const { result } = renderHook(() => usePluginContext<AuthContext>('auth'), { wrapper });
      expect(result.current?.user).toBe('admin');
      expect(result.current?.isAuthenticated).toBe(true);
    });
  });
});
