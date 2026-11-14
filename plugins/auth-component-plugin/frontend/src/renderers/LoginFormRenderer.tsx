import React, { useState } from 'react';
import type { RendererProps } from '../types';
import '../styles/AuthComponents.css';
import { useRuntimeAuth } from '../runtimeBridge';

/**
 * LoginForm Renderer
 * Renders a login form with email/password fields and optional features
 */
const LoginFormRenderer: React.FC<RendererProps> = ({ component, isEditMode }) => {
  const props = component.props || {};
  const styles = component.styles || {};
  const auth = useRuntimeAuth();

  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [rememberMe, setRememberMe] = useState(false);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (isEditMode) return;

    setIsLoading(true);
    setError(null);

    try {
      // Live runtime: sign in through the server session. The identity then reaches every
      // plugin on the page through shared state, so the page does not need to reload.
      if (auth.login) {
        const result = await auth.login(email, password);
        if (!result.ok) {
          setError(result.message || 'Invalid email or password');
        } else {
          const target = props.redirectUrl as string | undefined;
          if (target && target !== window.location.pathname) {
            window.location.href = target;
          }
        }
        return;
      }

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
        setError(data.message || 'Invalid email or password');
      }
    } catch {
      setError('An error occurred. Please try again.');
    } finally {
      setIsLoading(false);
    }
  };

  const containerStyle: React.CSSProperties = {
    backgroundColor: styles.backgroundColor || '#ffffff',
    borderRadius: styles.borderRadius || '12px',
    padding: styles.padding || '32px',
    boxShadow: styles.boxShadow || '0 4px 6px rgba(0, 0, 0, 0.1)',
    maxWidth: styles.maxWidth || '400px',
    width: '100%',
  };

  // Live runtime: the session already has an identity, so show it instead of the form.
  if (auth.user && !isEditMode) {
    return (
      <div className="auth-form login-form" style={containerStyle} data-signed-in="true">
        <h2 className="auth-form-title">Signed in</h2>
        <p className="auth-form-subtitle">You are signed in as {auth.user.displayName}.</p>
      </div>
    );
  }

  const title = props.title as string | undefined;
  const subtitle = props.subtitle as string | undefined;

  // Live runtime with single sign-on: a button that goes to the VSD auth server. The password
  // form stays unless the site turned it off.
  const showSso = !isEditMode && auth.ssoEnabled && !!auth.startSso;
  const showPasswordForm = isEditMode || !showSso || auth.passwordLogin !== false;

  return (
    <div className="auth-form login-form" style={containerStyle}>
      {title && <h2 className="auth-form-title">{title}</h2>}
      {subtitle && <p className="auth-form-subtitle">{subtitle}</p>}

      {showSso && (
        <button
          type="button"
          className="auth-form-submit"
          data-sso="true"
          onClick={() => auth.startSso?.()}
        >
          {props.ssoButtonText as string || 'Sign in with VSD'}
        </button>
      )}
      {showSso && showPasswordForm && (
        <p className="auth-form-footer">{props.ssoDividerText as string || 'or use your email and password'}</p>
      )}

      {showPasswordForm && (
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
      )}
    </div>
  );
};

export default LoginFormRenderer;
