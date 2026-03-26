import React, { useState } from 'react';
import type { RendererProps } from '../types';
import { useAuthContext } from '../hooks/useAuthContext';
import '../styles/AuthComponents.css';

/**
 * LoginForm Renderer
 * Renders a login form with email/password fields and optional features.
 * When auth-context-plugin is installed, uses shared auth state so other
 * components (LogoutButton, navbar, etc.) react to login automatically.
 */
const LoginFormRenderer: React.FC<RendererProps> = ({ component, isEditMode }) => {
  const props = component.props || {};
  const styles = component.styles || {};

  const auth = useAuthContext();

  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [rememberMe, setRememberMe] = useState(false);
  const [localLoading, setLocalLoading] = useState(false);
  const [localError, setLocalError] = useState<string | null>(null);

  // Use context state when available, otherwise use local state
  const isLoading = auth ? auth.isLoading : localLoading;
  const error = auth ? auth.error : localError;

  // If user is already authenticated via context, show a message
  if (auth?.isAuthenticated && !isEditMode) {
    return (
      <div className="auth-form login-form" style={containerStyle(styles)}>
        <div className="auth-success">
          <div className="auth-success-icon">✓</div>
          <h2 className="auth-form-title">Welcome back!</h2>
          <p className="auth-form-subtitle">
            You are signed in as {auth.user?.name || auth.user?.email}.
          </p>
        </div>
      </div>
    );
  }

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (isEditMode) return;

    if (auth) {
      // Use shared auth context — other components will react automatically
      try {
        await auth.login(email, password);
        // No redirect needed — state updates propagate to all components
        const redirectUrl = props.redirectUrl as string;
        if (redirectUrl) {
          window.location.href = redirectUrl;
        }
      } catch {
        // Error is already set in auth context
      }
    } else {
      // Fallback: direct API call (no auth-context-plugin installed)
      setLocalLoading(true);
      setLocalError(null);

      try {
        const response = await fetch(props.loginEndpoint as string || '/api/auth/login', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ email, password, rememberMe }),
          credentials: 'include',
        });

        if (response.ok) {
          window.location.href = props.redirectUrl as string || '/';
        } else {
          const data = await response.json();
          setLocalError(data.message || 'Invalid email or password');
        }
      } catch {
        setLocalError('An error occurred. Please try again.');
      } finally {
        setLocalLoading(false);
      }
    }
  };

  const title = props.title as string | undefined;
  const subtitle = props.subtitle as string | undefined;

  return (
    <div className="auth-form login-form" style={containerStyle(styles)}>
      {title && <h2 className="auth-form-title">{title}</h2>}
      {subtitle && <p className="auth-form-subtitle">{subtitle}</p>}

      <form onSubmit={handleSubmit} className="auth-form-content">
        {error && <div className="auth-form-error">{error}</div>}

        <div className="auth-form-field">
          <label htmlFor="email">{props.emailLabel as string || 'Email'}</label>
          <input
            type="email"
            id="email"
            placeholder={props.emailPlaceholder as string || 'Enter your email'}
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            disabled={isEditMode || isLoading}
            required
          />
        </div>

        <div className="auth-form-field">
          <label htmlFor="password">{props.passwordLabel as string || 'Password'}</label>
          <input
            type="password"
            id="password"
            placeholder={props.passwordPlaceholder as string || 'Enter your password'}
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            disabled={isEditMode || isLoading}
            required
          />
        </div>

        <div className="auth-form-options">
          {props.showRememberMe !== false && (
            <label className="auth-checkbox">
              <input
                type="checkbox"
                checked={rememberMe}
                onChange={(e) => setRememberMe(e.target.checked)}
                disabled={isEditMode || isLoading}
              />
              <span>{props.rememberMeLabel as string || 'Remember me'}</span>
            </label>
          )}

          {props.showForgotPassword !== false && (
            <a
              href={props.forgotPasswordUrl as string || '/forgot-password'}
              className="auth-form-link"
              onClick={(e) => isEditMode && e.preventDefault()}
            >
              {props.forgotPasswordLabel as string || 'Forgot password?'}
            </a>
          )}
        </div>

        <button
          type="submit"
          className="auth-form-submit"
          disabled={isEditMode || isLoading}
        >
          {isLoading ? 'Signing in...' : (props.submitButtonText as string || 'Sign In')}
        </button>

        {props.showSignUpLink !== false && (
          <p className="auth-form-footer">
            {props.signUpText as string || "Don't have an account?"}{' '}
            <a
              href={props.signUpUrl as string || '/register'}
              className="auth-form-link"
              onClick={(e) => isEditMode && e.preventDefault()}
            >
              {props.signUpLinkText as string || 'Sign up'}
            </a>
          </p>
        )}
      </form>
    </div>
  );
};

function containerStyle(styles: Record<string, string>): React.CSSProperties {
  return {
    backgroundColor: styles.backgroundColor || '#ffffff',
    borderRadius: styles.borderRadius || '12px',
    padding: styles.padding || '32px',
    boxShadow: styles.boxShadow || '0 4px 6px rgba(0, 0, 0, 0.1)',
    maxWidth: styles.maxWidth || '400px',
    width: '100%',
  };
}

export default LoginFormRenderer;
