import React from 'react';
import type { RendererProps } from '../types';
import { useAuthContext } from '../hooks/useAuthContext';

/**
 * UserProfileBadgeRenderer
 *
 * Displays the logged-in user's avatar and name, or a "Sign In" link
 * when not authenticated. Designed to be placed inside a Navbar or header.
 *
 * Variants:
 * - compact: avatar + name side by side
 * - full: avatar + name + logout button
 * - avatar-only: just the avatar circle
 *
 * Gracefully degrades when auth-context-plugin is not installed
 * (shows a static placeholder).
 */
const UserProfileBadgeRenderer: React.FC<RendererProps> = ({ component, isEditMode }) => {
  const props = component.props || {};
  const styles = component.styles || {};

  const showAvatar = props.showAvatar !== false;
  const showName = props.showName !== false;
  const avatarSize = parseInt(String(props.avatarSize || '32'), 10);
  const variant = String(props.variant || 'compact');
  const showLogoutButton = props.showLogoutButton === true || variant === 'full';
  const loginText = String(props.loginText || 'Sign In');
  const logoutText = String(props.logoutText || 'Sign Out');
  const loginUrl = String(props.loginUrl || '/login');
  const showGreeting = props.showGreeting === true;
  const greeting = String(props.greeting || 'Hello,');

  const auth = useAuthContext();

  // Container style
  const containerStyle: React.CSSProperties = {
    display: 'flex',
    alignItems: 'center',
    gap: styles.gap || '8px',
    fontSize: styles.fontSize || '14px',
    color: styles.color || '#333',
    cursor: 'default',
  };

  // Edit mode: show placeholder badge
  if (isEditMode) {
    return (
      <div className="user-profile-badge user-profile-badge--edit" style={containerStyle}>
        {showAvatar && (
          <div
            className="user-profile-badge__avatar user-profile-badge__avatar--placeholder"
            style={{
              width: avatarSize,
              height: avatarSize,
              borderRadius: '50%',
              backgroundColor: '#e0e0e0',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              fontSize: avatarSize * 0.5,
              color: '#999',
              flexShrink: 0,
            }}
          >
            <span role="img" aria-label="user">&#128100;</span>
          </div>
        )}
        {variant !== 'avatar-only' && showName && (
          <span className="user-profile-badge__name">
            {showGreeting && <span className="user-profile-badge__greeting">{greeting} </span>}
            John Doe
          </span>
        )}
        {showLogoutButton && variant !== 'avatar-only' && (
          <button className="user-profile-badge__logout" disabled style={{
            background: 'none',
            border: '1px solid #ccc',
            borderRadius: '4px',
            padding: '4px 8px',
            fontSize: '12px',
            color: '#666',
            cursor: 'not-allowed',
          }}>
            {logoutText}
          </button>
        )}
      </div>
    );
  }

  // No auth context available: show static placeholder or nothing
  if (!auth) {
    return (
      <div className="user-profile-badge user-profile-badge--no-context" style={containerStyle}>
        <a
          href={loginUrl}
          className="user-profile-badge__login-link"
          style={{
            color: 'inherit',
            textDecoration: 'none',
            fontWeight: 500,
          }}
        >
          {loginText}
        </a>
      </div>
    );
  }

  // Loading state
  if (auth.isLoading) {
    return (
      <div className="user-profile-badge user-profile-badge--loading" style={containerStyle}>
        <div
          className="user-profile-badge__skeleton"
          style={{
            width: avatarSize,
            height: avatarSize,
            borderRadius: '50%',
            backgroundColor: '#e0e0e0',
            animation: 'pulse 1.5s ease-in-out infinite',
            flexShrink: 0,
          }}
        />
        {variant !== 'avatar-only' && (
          <div
            style={{
              width: 80,
              height: 14,
              borderRadius: '4px',
              backgroundColor: '#e0e0e0',
              animation: 'pulse 1.5s ease-in-out infinite',
            }}
          />
        )}
      </div>
    );
  }

  // Not authenticated: show login link
  if (!auth.isAuthenticated || !auth.user) {
    return (
      <div className="user-profile-badge user-profile-badge--guest" style={containerStyle}>
        <a
          href={loginUrl}
          className="user-profile-badge__login-link"
          onClick={(e) => {
            // If auth context has providers, use OAuth2 login
            if ('providers' in auth && Array.isArray((auth as any).providers) && (auth as any).providers.length > 0) {
              e.preventDefault();
              auth.login();
            }
          }}
          style={{
            color: 'inherit',
            textDecoration: 'none',
            fontWeight: 500,
            display: 'flex',
            alignItems: 'center',
            gap: '6px',
          }}
        >
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
            <path d="M15 3h4a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2h-4" />
            <polyline points="10 17 15 12 10 7" />
            <line x1="15" y1="12" x2="3" y2="12" />
          </svg>
          {loginText}
        </a>
      </div>
    );
  }

  // Authenticated: show user info
  const user = auth.user;
  const initials = user.name
    ? user.name.split(' ').map(n => n[0]).join('').toUpperCase().slice(0, 2)
    : user.email?.[0]?.toUpperCase() || '?';

  return (
    <div className="user-profile-badge user-profile-badge--authenticated" style={containerStyle}>
      {showAvatar && (
        user.avatarUrl ? (
          <img
            src={user.avatarUrl}
            alt={user.name || 'User avatar'}
            className="user-profile-badge__avatar"
            style={{
              width: avatarSize,
              height: avatarSize,
              borderRadius: '50%',
              objectFit: 'cover',
              flexShrink: 0,
            }}
          />
        ) : (
          <div
            className="user-profile-badge__avatar user-profile-badge__avatar--initials"
            style={{
              width: avatarSize,
              height: avatarSize,
              borderRadius: '50%',
              backgroundColor: '#4f46e5',
              color: '#ffffff',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              fontSize: avatarSize * 0.4,
              fontWeight: 600,
              flexShrink: 0,
            }}
          >
            {initials}
          </div>
        )
      )}

      {variant !== 'avatar-only' && showName && (
        <span className="user-profile-badge__name" style={{ fontWeight: 500 }}>
          {showGreeting && <span className="user-profile-badge__greeting" style={{ fontWeight: 400 }}>{greeting} </span>}
          {user.name || user.email}
        </span>
      )}

      {showLogoutButton && variant !== 'avatar-only' && (
        <button
          className="user-profile-badge__logout"
          onClick={() => auth.logout()}
          style={{
            background: 'none',
            border: '1px solid #d1d5db',
            borderRadius: '4px',
            padding: '4px 10px',
            fontSize: '12px',
            color: '#6b7280',
            cursor: 'pointer',
            transition: 'all 0.15s ease',
          }}
          onMouseEnter={(e) => {
            e.currentTarget.style.borderColor = '#ef4444';
            e.currentTarget.style.color = '#ef4444';
          }}
          onMouseLeave={(e) => {
            e.currentTarget.style.borderColor = '#d1d5db';
            e.currentTarget.style.color = '#6b7280';
          }}
        >
          {logoutText}
        </button>
      )}
    </div>
  );
};

export default UserProfileBadgeRenderer;
