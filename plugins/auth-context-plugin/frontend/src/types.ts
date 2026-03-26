export interface AuthUser {
  id: string;
  email: string;
  name: string;
  avatarUrl?: string;
  provider?: string;
}

export interface AuthProvider {
  id: string;
  name: string;
  authorizationUrl: string;
}

export interface AuthContextValue {
  isAuthenticated: boolean;
  user: AuthUser | null;
  isLoading: boolean;
  error: string | null;
  providers: AuthProvider[];
  login: (providerId?: string) => void;
  logout: () => Promise<void>;
  clearError: () => void;
}
