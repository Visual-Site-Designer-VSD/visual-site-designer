import api from './api';

/** The VSD Auth Server settings as the server reports them. The client secret is never included. */
export interface AuthServerSettings {
  enabled: boolean;
  /** Complete enough that people can sign in. */
  usable: boolean;
  /** "database": set in the admin console. "environment": still the application configuration. */
  source: 'database' | 'environment';
  issuerUri: string | null;
  authorizationUri: string | null;
  tokenUri: string | null;
  jwkSetUri: string | null;
  userInfoUri: string | null;
  clientId: string | null;
  clientSecretSet: boolean;
  scopes: string;
  /** The address the auth server has to be told to send people back to. */
  redirectUri: string;
}

/** What an administrator submits. A blank client secret keeps the saved one. */
export interface AuthServerUpdate {
  enabled: boolean;
  issuerUri: string;
  authorizationUri: string;
  tokenUri: string;
  jwkSetUri: string;
  userInfoUri: string;
  clientId: string;
  clientSecret: string;
  scopes: string;
}

export interface AuthServerCheck {
  name: string;
  ok: boolean;
  message: string;
}

/** The message the server gives for a refused request, or a general one. */
export function authServerError(error: unknown): string {
  const e = error as { response?: { data?: { error?: string; message?: string } }; message?: string };
  return e?.response?.data?.error ?? e?.response?.data?.message ?? e?.message ?? 'Something went wrong.';
}

export const authServerService = {
  get: async (): Promise<AuthServerSettings> => (await api.get<AuthServerSettings>('/admin/auth-server')).data,

  save: async (update: AuthServerUpdate): Promise<AuthServerSettings> =>
    (await api.put<AuthServerSettings>('/admin/auth-server', update)).data,

  /** Forgets the saved settings so the application configuration applies again. */
  reset: async (): Promise<AuthServerSettings> => (await api.delete<AuthServerSettings>('/admin/auth-server')).data,

  /** Looks at the addresses without saving anything or signing anybody in. */
  test: async (update: AuthServerUpdate): Promise<{ ok: boolean; checks: AuthServerCheck[] }> =>
    (await api.post<{ ok: boolean; checks: AuthServerCheck[] }>('/admin/auth-server/test', update)).data,

  /** Public: should the sign-in page offer the VSD Auth Server? */
  publicConfig: async (): Promise<{ ssoEnabled: boolean; ssoName: string }> =>
    (await api.get<{ ssoEnabled: boolean; ssoName: string }>('/auth/config')).data,
};
