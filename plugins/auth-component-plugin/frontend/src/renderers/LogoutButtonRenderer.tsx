import React, { useState } from 'react';
import type { RendererProps } from '../types';
import { useAuthContext } from '../hooks/useAuthContext';
import '../styles/AuthComponents.css';

/**
 * LogoutButton Renderer
 * Renders a logout button with optional confirmation dialog.
 * When auth-context-plugin is installed, uses shared auth state to
 * conditionally show/hide and to trigger logout across all components.
 */
const LogoutButtonRenderer: React.FC<RendererProps> = ({ component, isEditMode }) => {
  const props = component.props || {};
  const styles = component.styles || {};

  const auth = useAuthContext();

  const [localLoading, setLocalLoading] = useState(false);
  const [showConfirm, setShowConfirm] = useState(false);

  const isLoading = auth ? auth.isLoading : localLoading;

  // When auth context is available, respect showOnlyWhenLoggedIn
  // In edit mode, always show for design purposes
  if (!isEditMode && props.showOnlyWhenLoggedIn) {
    if (auth && !auth.isAuthenticated) {
      return null;
    }
  }

  const handleLogout = async () => {
    if (isEditMode) return;

    if (props.confirmLogout && !showConfirm) {
      setShowConfirm(true);
      return;
    }

    if (auth) {
      // Use shared auth context — LoginForm, navbar, etc. will react automatically
      await auth.logout();
      const redirectUrl = props.redirectUrl as string;
      if (redirectUrl) {
        window.location.href = redirectUrl;
      }
    } else {
      // Fallback: direct API call (no auth-context-plugin installed)
      setLocalLoading(true);

      try {
        const response = await fetch((props.logoutEndpoint as string) || '/api/auth/logout', {
          method: 'POST',
          credentials: 'include',
        });

        if (response.ok || response.status === 204) {
          window.location.href = (props.redirectUrl as string) || '/';
        }
      } catch (err) {
        console.error('Logout failed:', err);
        window.location.href = (props.redirectUrl as string) || '/';
      } finally {
        setLocalLoading(false);
      }
    }
  };

  const getVariantStyles = (): React.CSSProperties => {
    const variant = (props.variant as string) || 'secondary';
    const baseStyles: React.CSSProperties = {
      display: 'inline-flex',
      alignItems: 'center',
      justifyContent: 'center',
      gap: '8px',
      borderRadius: styles.borderRadius || '8px',
      fontWeight: 500,
      cursor: isEditMode ? 'default' : 'pointer',
      transition: 'all 0.2s ease',
      border: 'none',
    };

    const size = (props.size as string) || 'medium';
    const sizeStyles: Record<string, React.CSSProperties> = {
      small: { padding: '6px 12px', fontSize: '12px' },
      medium: { padding: '10px 20px', fontSize: '14px' },
      large: { padding: '14px 28px', fontSize: '16px' },
    };

    const variantStyles: Record<string, React.CSSProperties> = {
      primary: {
        backgroundColor: styles.backgroundColor || '#007bff',
        color: styles.textColor || '#ffffff',
      },
      secondary: {
        backgroundColor: styles.backgroundColor || '#6c757d',
        color: styles.textColor || '#ffffff',
      },
      danger: {
        backgroundColor: styles.backgroundColor || '#dc3545',
        color: styles.textColor || '#ffffff',
      },
      text: {
        backgroundColor: 'transparent',
        color: styles.textColor || '#6c757d',
      },
      outlined: {
        backgroundColor: 'transparent',
        color: styles.textColor || '#6c757d',
        border: `2px solid ${styles.borderColor || '#6c757d'}`,
      },
    };

    return {
      ...baseStyles,
      ...sizeStyles[size],
      ...variantStyles[variant],
    };
  };

  const iconPosition = (props.iconPosition as string) || 'left';
  const showIcon = props.showIcon !== false;
  const icon = (props.icon as string) || '🚪';

  return (
    <>
      <button
        className={`logout-button logout-${(props.variant as string) || 'secondary'}`}
        style={getVariantStyles()}
        onClick={handleLogout}
        disabled={isEditMode || isLoading}
      >
        {showIcon && iconPosition === 'left' && (
          <span className="logout-icon">{icon}</span>
        )}
        <span className="logout-text">
          {isLoading ? 'Signing out...' : (props.text as string) || 'Sign Out'}
        </span>
        {showIcon && iconPosition === 'right' && (
          <span className="logout-icon">{icon}</span>
        )}
      </button>

      {/* Confirmation Dialog */}
      {showConfirm && (
        <div className="logout-confirm-overlay" onClick={() => setShowConfirm(false)}>
          <div
            className="logout-confirm-dialog"
            onClick={(e) => e.stopPropagation()}
          >
            <h3>{(props.confirmTitle as string) || 'Sign Out'}</h3>
            <p>{(props.confirmMessage as string) || 'Are you sure you want to sign out?'}</p>
            <div className="logout-confirm-actions">
              <button
                className="logout-confirm-cancel"
                onClick={() => setShowConfirm(false)}
              >
                {(props.cancelButtonText as string) || 'Cancel'}
              </button>
              <button
                className="logout-confirm-submit"
                onClick={handleLogout}
                disabled={isLoading}
              >
                {isLoading ? 'Signing out...' : (props.confirmButtonText as string) || 'Sign Out'}
              </button>
            </div>
          </div>
        </div>
      )}
    </>
  );
};

export default LogoutButtonRenderer;
