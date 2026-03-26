import React, { useState, useCallback, useEffect } from 'react';
import type { AuthUser, AuthProvider as AuthProviderType, AuthContextValue } from './types';

const API_BASE = '/api/ctx/auth';

interface AuthProviderProps {
  children: React.ReactNode;
  onContextReady?: (value: AuthContextValue) => void;
}

/**
 * AuthProvider - React context provider for shared authentication state.
 *
 * Integrates with Spring Security OAuth2 on the backend:
 * - Checks session on mount via GET /api/ctx/auth/session
 * - Fetches available OAuth2 providers via GET /api/ctx/auth/providers
 * - Login redirects the browser to Spring Security's OAuth2 authorization endpoint
 * - Logout calls POST /api/ctx/auth/logout (handled by Spring Security)
 * - After OAuth2 callback, the SPA detects ?authenticated=true and re-checks session
 */
export const AuthProvider: React.FC<AuthProviderProps> = ({ children, onContextReady }) => {
  const [user, setUser] = useState<AuthUser | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [providers, setProviders] = useState<AuthProviderType[]>([]);

  const isAuthenticated = user !== null;

  const clearError = useCallback(() => setError(null), []);

  /**
   * Redirect to OAuth2 authorization endpoint.
   * If no providerId is specified, redirects to the first available provider.
   */
  const login = useCallback((providerId?: string) => {
    const provider = providerId
      ? providers.find(p => p.id === providerId)
      : providers[0];

    if (provider) {
      window.location.href = provider.authorizationUrl;
    } else {
      setError('No authentication providers available');
    }
  }, [providers]);

  const logout = useCallback(async () => {
    setIsLoading(true);
    try {
      await fetch(`${API_BASE}/logout`, {
        method: 'POST',
        credentials: 'same-origin',
      });
      setUser(null);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Logout failed');
    } finally {
      setIsLoading(false);
    }
  }, []);

  // Check existing session on mount
  useEffect(() => {
    const checkSession = async () => {
      try {
        const response = await fetch(`${API_BASE}/session`, {
          credentials: 'same-origin',
        });
        if (response.ok) {
          const data = await response.json();
          if (data.authenticated && data.user) {
            setUser(data.user);
          }
        }
      } catch {
        // No active session
      } finally {
        setIsLoading(false);
      }
    };
    checkSession();

    // If returning from OAuth2 callback, clean up the URL
    const params = new URLSearchParams(window.location.search);
    if (params.has('authenticated')) {
      const url = new URL(window.location.href);
      url.searchParams.delete('authenticated');
      window.history.replaceState({}, '', url.pathname + url.search);
    }
  }, []);

  // Fetch available OAuth2 providers
  useEffect(() => {
    const fetchProviders = async () => {
      try {
        const response = await fetch(`${API_BASE}/providers`, {
          credentials: 'same-origin',
        });
        if (response.ok) {
          const data = await response.json();
          setProviders(data.providers || []);
        }
      } catch {
        // Provider listing failed - login buttons won't show
      }
    };
    fetchProviders();
  }, []);

  // Notify parent when context value changes
  const contextValue: AuthContextValue = {
    isAuthenticated,
    user,
    isLoading,
    error,
    providers,
    login,
    logout,
    clearError,
  };

  useEffect(() => {
    if (onContextReady) {
      onContextReady(contextValue);
    }
  }, [isAuthenticated, user, isLoading, error, providers]);

  return <>{children}</>;
};

export default AuthProvider;
