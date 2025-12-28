import React, { useState } from 'react';
import type { RendererProps } from '../types';
import '../styles/AuthComponents.css';

/**
 * LogoutButton Renderer
 * Renders a logout button with optional confirmation dialog
 */
const LogoutButtonRenderer: React.FC<RendererProps> = ({ component, isEditMode }) => {
  const props = component.props || {};
  const styles = component.styles || {};

  const [isLoading, setIsLoading] = useState(false);
  const [showConfirm, setShowConfirm] = useState(false);

  const handleLogout = async () => {
    if (isEditMode) return;

    if (props.confirmLogout && !showConfirm) {
      setShowConfirm(true);
      return;
    }

    setIsLoading(true);

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
      // Still redirect on error - server session may have been cleared
      window.location.href = (props.redirectUrl as string) || '/';
    } finally {
      setIsLoading(false);
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

    // Size styles
    const size = (props.size as string) || 'medium';
    const sizeStyles: Record<string, React.CSSProperties> = {
      small: { padding: '6px 12px', fontSize: '12px' },
      medium: { padding: '10px 20px', fontSize: '14px' },
      large: { padding: '14px 28px', fontSize: '16px' },
    };

    // Variant styles
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

  // Note: In a real app, this would check the actual auth state
  // For the builder preview, we always show the button
  // The showOnlyWhenLoggedIn prop would be handled by the runtime
  const shouldShow = isEditMode || !props.showOnlyWhenLoggedIn;

  if (!shouldShow) {
    return null;
  }

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
