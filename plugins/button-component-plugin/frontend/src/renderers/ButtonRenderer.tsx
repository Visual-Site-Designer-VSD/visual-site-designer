import React, { useState } from 'react';
import type { RendererProps } from '../types';

/**
 * Props for the Button component.
 * Extends RendererProps for plugin compatibility.
 */
export interface ButtonProps extends RendererProps {
  /** Button text */
  text?: string;
  /** Button variant style */
  variant?: 'primary' | 'secondary' | 'success' | 'danger' | 'warning' | 'outline' | 'outline-light' | 'link';
  /** Button size */
  size?: 'small' | 'medium' | 'large';
  /** Disable the button */
  disabled?: boolean;
  /** Make button full width */
  fullWidth?: boolean;
  /** Custom inline styles */
  style?: React.CSSProperties;
  /** Border radius */
  borderRadius?: string;
  /** Font weight */
  fontWeight?: string | number;
  /** Click handler */
  onClick?: (e: React.MouseEvent) => void;
}

/**
 * ButtonRenderer - Renders a button component with its props
 */
const ButtonRenderer: React.FC<ButtonProps> = (props) => {
  const [isHovered, setIsHovered] = useState(false);

  const { component, isEditMode } = props;
  const cp = component.props;

  const text = (props.text ?? cp?.text as string) ?? 'Click Me';
  const variant = (props.variant ?? cp?.variant as string) ?? 'primary';
  const size = (props.size ?? cp?.size as string) ?? 'medium';
  const disabled = (props.disabled ?? cp?.disabled as boolean) ?? false;
  const fullWidth = (props.fullWidth ?? cp?.fullWidth as boolean) ?? false;

  // Get custom styles: direct props override component.styles
  const customStyles: React.CSSProperties = {
    ...(component.styles as React.CSSProperties),
    borderRadius: props.borderRadius,
    fontWeight: props.fontWeight,
    ...props.style,
  };

  const variantStyles: Record<string, React.CSSProperties> = {
    primary: {
      backgroundColor: '#007bff',
      color: 'white',
      border: 'none',
    },
    secondary: {
      backgroundColor: '#6c757d',
      color: 'white',
      border: 'none',
    },
    success: {
      backgroundColor: '#28a745',
      color: 'white',
      border: 'none',
    },
    danger: {
      backgroundColor: '#dc3545',
      color: 'white',
      border: 'none',
    },
    warning: {
      backgroundColor: '#ffc107',
      color: '#212529',
      border: 'none',
    },
    outline: {
      backgroundColor: 'transparent',
      color: '#007bff',
      border: '2px solid #007bff',
    },
    'outline-light': {
      backgroundColor: 'transparent',
      color: '#ffffff',
      border: '2px solid #ffffff',
    },
    link: {
      backgroundColor: 'transparent',
      color: '#007bff',
      border: 'none',
      textDecoration: 'underline',
    },
  };

  const sizeStyles: Record<string, React.CSSProperties> = {
    small: {
      padding: '6px 12px',
      fontSize: '13px',
    },
    medium: {
      padding: '8px 16px',
      fontSize: '14px',
    },
    large: {
      padding: '12px 24px',
      fontSize: '16px',
    },
  };

  const hoverStyles: Record<string, React.CSSProperties> = {
    primary: { backgroundColor: '#0056b3' },
    secondary: { backgroundColor: '#545b62' },
    success: { backgroundColor: '#218838' },
    danger: { backgroundColor: '#c82333' },
    warning: { backgroundColor: '#e0a800' },
    outline: { backgroundColor: '#007bff', color: 'white' },
    'outline-light': { backgroundColor: '#ffffff', color: '#007bff' },
    link: { textDecoration: 'none' },
  };

  const baseStyles: React.CSSProperties = {
    display: 'block',
    fontWeight: 500,
    textAlign: 'center',
    whiteSpace: 'nowrap',
    overflow: 'hidden',
    textOverflow: 'ellipsis',
    verticalAlign: 'middle',
    userSelect: 'none',
    borderRadius: '6px',
    transition: 'all 0.2s',
    cursor: disabled ? 'not-allowed' : (isEditMode ? 'default' : 'pointer'),
    opacity: disabled ? 0.65 : 1,
    width: fullWidth ? '100%' : 'auto',
    minWidth: 0,
    boxSizing: 'border-box',
    outline: 'none',
  };

  const buttonStyles: React.CSSProperties = {
    ...baseStyles,
    ...(variantStyles[variant as string] || variantStyles.primary),
    ...(sizeStyles[size as string] || sizeStyles.medium),
    ...(isHovered && !disabled ? hoverStyles[variant as string] : {}),
    ...Object.fromEntries(
      Object.entries(customStyles).filter(([, value]) => value !== undefined)
    ),
  };

  const handleClick = (e: React.MouseEvent) => {
    if (isEditMode) {
      e.preventDefault();
      return;
    }
    if (props.onClick) {
      props.onClick(e);
    }
  };

  return (
    <button
      style={buttonStyles}
      disabled={disabled as boolean}
      onClick={handleClick}
      onMouseEnter={() => setIsHovered(true)}
      onMouseLeave={() => setIsHovered(false)}
      type="button"
    >
      {text as string}
    </button>
  );
};

export default ButtonRenderer;
export { ButtonRenderer };
