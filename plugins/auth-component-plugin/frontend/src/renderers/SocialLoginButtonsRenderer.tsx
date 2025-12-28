import React from 'react';
import type { RendererProps } from '../types';
import '../styles/AuthComponents.css';

/**
 * SocialLoginButtons Renderer
 * Renders social login buttons for various providers (Google, GitHub, Facebook, etc.)
 */
const SocialLoginButtonsRenderer: React.FC<RendererProps> = ({ component, isEditMode }) => {
  const props = component.props || {};
  const styles = component.styles || {};

  const handleSocialLogin = (url: string) => {
    if (isEditMode) return;
    window.location.href = url;
  };

  // Provider configurations with icons and colors
  const providers = [
    {
      id: 'google',
      name: 'Google',
      icon: (
        <svg viewBox="0 0 24 24" width="20" height="20">
          <path fill="#4285F4" d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z"/>
          <path fill="#34A853" d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z"/>
          <path fill="#FBBC05" d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l2.85-2.22.81-.62z"/>
          <path fill="#EA4335" d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z"/>
        </svg>
      ),
      bgColor: '#ffffff',
      textColor: '#757575',
      borderColor: '#dadce0',
      show: props.showGoogle,
      url: (props.googleAuthUrl as string) || '/oauth2/authorization/google',
      text: (props.googleText as string) || 'Continue with Google',
    },
    {
      id: 'github',
      name: 'GitHub',
      icon: (
        <svg viewBox="0 0 24 24" width="20" height="20" fill="currentColor">
          <path d="M12 0c-6.626 0-12 5.373-12 12 0 5.302 3.438 9.8 8.207 11.387.599.111.793-.261.793-.577v-2.234c-3.338.726-4.033-1.416-4.033-1.416-.546-1.387-1.333-1.756-1.333-1.756-1.089-.745.083-.729.083-.729 1.205.084 1.839 1.237 1.839 1.237 1.07 1.834 2.807 1.304 3.492.997.107-.775.418-1.305.762-1.604-2.665-.305-5.467-1.334-5.467-5.931 0-1.311.469-2.381 1.236-3.221-.124-.303-.535-1.524.117-3.176 0 0 1.008-.322 3.301 1.23.957-.266 1.983-.399 3.003-.404 1.02.005 2.047.138 3.006.404 2.291-1.552 3.297-1.23 3.297-1.23.653 1.653.242 2.874.118 3.176.77.84 1.235 1.911 1.235 3.221 0 4.609-2.807 5.624-5.479 5.921.43.372.823 1.102.823 2.222v3.293c0 .319.192.694.801.576 4.765-1.589 8.199-6.086 8.199-11.386 0-6.627-5.373-12-12-12z"/>
        </svg>
      ),
      bgColor: '#24292e',
      textColor: '#ffffff',
      borderColor: '#24292e',
      show: props.showGithub,
      url: (props.githubAuthUrl as string) || '/oauth2/authorization/github',
      text: (props.githubText as string) || 'Continue with GitHub',
    },
    {
      id: 'facebook',
      name: 'Facebook',
      icon: (
        <svg viewBox="0 0 24 24" width="20" height="20" fill="currentColor">
          <path d="M24 12.073c0-6.627-5.373-12-12-12s-12 5.373-12 12c0 5.99 4.388 10.954 10.125 11.854v-8.385H7.078v-3.47h3.047V9.43c0-3.007 1.792-4.669 4.533-4.669 1.312 0 2.686.235 2.686.235v2.953H15.83c-1.491 0-1.956.925-1.956 1.874v2.25h3.328l-.532 3.47h-2.796v8.385C19.612 23.027 24 18.062 24 12.073z"/>
        </svg>
      ),
      bgColor: '#1877F2',
      textColor: '#ffffff',
      borderColor: '#1877F2',
      show: props.showFacebook,
      url: (props.facebookAuthUrl as string) || '/oauth2/authorization/facebook',
      text: (props.facebookText as string) || 'Continue with Facebook',
    },
    {
      id: 'twitter',
      name: 'Twitter',
      icon: (
        <svg viewBox="0 0 24 24" width="20" height="20" fill="currentColor">
          <path d="M18.244 2.25h3.308l-7.227 8.26 8.502 11.24H16.17l-5.214-6.817L4.99 21.75H1.68l7.73-8.835L1.254 2.25H8.08l4.713 6.231zm-1.161 17.52h1.833L7.084 4.126H5.117z"/>
        </svg>
      ),
      bgColor: '#000000',
      textColor: '#ffffff',
      borderColor: '#000000',
      show: props.showTwitter,
      url: (props.twitterAuthUrl as string) || '/oauth2/authorization/twitter',
      text: (props.twitterText as string) || 'Continue with X',
    },
    {
      id: 'linkedin',
      name: 'LinkedIn',
      icon: (
        <svg viewBox="0 0 24 24" width="20" height="20" fill="currentColor">
          <path d="M20.447 20.452h-3.554v-5.569c0-1.328-.027-3.037-1.852-3.037-1.853 0-2.136 1.445-2.136 2.939v5.667H9.351V9h3.414v1.561h.046c.477-.9 1.637-1.85 3.37-1.85 3.601 0 4.267 2.37 4.267 5.455v6.286zM5.337 7.433c-1.144 0-2.063-.926-2.063-2.065 0-1.138.92-2.063 2.063-2.063 1.14 0 2.064.925 2.064 2.063 0 1.139-.925 2.065-2.064 2.065zm1.782 13.019H3.555V9h3.564v11.452zM22.225 0H1.771C.792 0 0 .774 0 1.729v20.542C0 23.227.792 24 1.771 24h20.451C23.2 24 24 23.227 24 22.271V1.729C24 .774 23.2 0 22.222 0h.003z"/>
        </svg>
      ),
      bgColor: '#0A66C2',
      textColor: '#ffffff',
      borderColor: '#0A66C2',
      show: props.showLinkedIn,
      url: (props.linkedinAuthUrl as string) || '/oauth2/authorization/linkedin',
      text: (props.linkedinText as string) || 'Continue with LinkedIn',
    },
    {
      id: 'apple',
      name: 'Apple',
      icon: (
        <svg viewBox="0 0 24 24" width="20" height="20" fill="currentColor">
          <path d="M12.152 6.896c-.948 0-2.415-1.078-3.96-1.04-2.04.027-3.91 1.183-4.961 3.014-2.117 3.675-.546 9.103 1.519 12.09 1.013 1.454 2.208 3.09 3.792 3.039 1.52-.065 2.09-.987 3.935-.987 1.831 0 2.35.987 3.96.948 1.637-.026 2.676-1.48 3.676-2.948 1.156-1.688 1.636-3.325 1.662-3.415-.039-.013-3.182-1.221-3.22-4.857-.026-3.04 2.48-4.494 2.597-4.559-1.429-2.09-3.623-2.324-4.39-2.376-2-.156-3.675 1.09-4.61 1.09zM15.53 3.83c.843-1.012 1.4-2.427 1.245-3.83-1.207.052-2.662.805-3.532 1.818-.78.896-1.454 2.338-1.273 3.714 1.338.104 2.715-.688 3.559-1.701z"/>
        </svg>
      ),
      bgColor: '#000000',
      textColor: '#ffffff',
      borderColor: '#000000',
      show: props.showApple,
      url: (props.appleAuthUrl as string) || '/oauth2/authorization/apple',
      text: (props.appleText as string) || 'Continue with Apple',
    },
    {
      id: 'microsoft',
      name: 'Microsoft',
      icon: (
        <svg viewBox="0 0 24 24" width="20" height="20">
          <path fill="#f25022" d="M1 1h10v10H1z"/>
          <path fill="#00a4ef" d="M1 13h10v10H1z"/>
          <path fill="#7fba00" d="M13 1h10v10H13z"/>
          <path fill="#ffb900" d="M13 13h10v10H13z"/>
        </svg>
      ),
      bgColor: '#ffffff',
      textColor: '#5e5e5e',
      borderColor: '#8c8c8c',
      show: props.showMicrosoft,
      url: (props.microsoftAuthUrl as string) || '/oauth2/authorization/microsoft',
      text: (props.microsoftText as string) || 'Continue with Microsoft',
    },
  ];

  const visibleProviders = providers.filter((p) => p.show !== false);
  const layout = (props.layout as string) || 'vertical';
  const buttonStyle = (props.buttonStyle as string) || 'filled';
  const showIcon = props.showIcon !== false;
  const showLabel = props.showLabel !== false;

  const containerStyle: React.CSSProperties = {
    display: 'flex',
    flexDirection: layout === 'vertical' ? 'column' : 'row',
    flexWrap: layout === 'grid' ? 'wrap' : 'nowrap',
    gap: styles.gap || '12px',
    width: styles.width || '100%',
    justifyContent: layout === 'horizontal' ? 'center' : 'stretch',
  };

  const getButtonStyle = (provider: typeof providers[0]): React.CSSProperties => {
    const base: React.CSSProperties = {
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'center',
      gap: '10px',
      padding: buttonStyle === 'icon-only' ? '12px' : '12px 16px',
      borderRadius: '8px',
      cursor: isEditMode ? 'default' : 'pointer',
      transition: 'all 0.2s ease',
      fontWeight: 500,
      fontSize: '14px',
      flex: layout === 'grid' ? '1 1 calc(50% - 6px)' : layout === 'horizontal' ? '0 0 auto' : '1',
      minWidth: buttonStyle === 'icon-only' ? '48px' : '120px',
    };

    if (buttonStyle === 'outlined') {
      return {
        ...base,
        backgroundColor: 'transparent',
        color: provider.textColor === '#ffffff' ? provider.bgColor : provider.textColor,
        border: `2px solid ${provider.borderColor}`,
      };
    }

    return {
      ...base,
      backgroundColor: provider.bgColor,
      color: provider.textColor,
      border: `1px solid ${provider.borderColor}`,
    };
  };

  if (visibleProviders.length === 0) {
    return (
      <div className="social-login-empty" style={{ textAlign: 'center', padding: '20px', color: '#888' }}>
        No social login providers enabled
      </div>
    );
  }

  return (
    <div className="social-login-buttons" style={containerStyle}>
      {props.showDivider !== false && (
        <div className="auth-social-divider">
          <span>{(props.dividerText as string) || 'or continue with'}</span>
        </div>
      )}

      <div
        style={{
          display: 'flex',
          flexDirection: layout === 'vertical' ? 'column' : 'row',
          flexWrap: layout === 'grid' ? 'wrap' : 'nowrap',
          gap: styles.gap || '12px',
          width: '100%',
        }}
      >
        {visibleProviders.map((provider) => (
          <button
            key={provider.id}
            className={`social-login-btn social-login-${provider.id}`}
            style={getButtonStyle(provider)}
            onClick={() => handleSocialLogin(provider.url)}
            disabled={isEditMode}
            title={provider.name}
          >
            {showIcon && <span className="social-login-icon">{provider.icon}</span>}
            {showLabel && buttonStyle !== 'icon-only' && (
              <span className="social-login-text">{provider.text}</span>
            )}
          </button>
        ))}
      </div>
    </div>
  );
};

export default SocialLoginButtonsRenderer;
