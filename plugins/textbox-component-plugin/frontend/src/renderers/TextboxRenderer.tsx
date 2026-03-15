import React, { useState } from 'react';
import type { ComponentInstance } from '../types';

/**
 * Props for the Textbox component.
 * component and isEditMode are optional for standalone usage (e.g., cross-plugin composition).
 */
export interface TextboxProps {
  /** Label text displayed above the input */
  label?: string;
  /** Show the label */
  showLabel?: boolean;
  /** Placeholder text */
  placeholder?: string;
  /** Input type: text, email, password, tel, url, number */
  type?: 'text' | 'email' | 'password' | 'tel' | 'url' | 'number';
  /** Use multiline textarea instead of input */
  multiline?: boolean;
  /** Number of rows for textarea (only when multiline) */
  rows?: number;
  /** Disable the input */
  disabled?: boolean;
  /** Make the input read-only */
  readOnly?: boolean;
  /** Mark as required field */
  required?: boolean;
  /** Maximum character length (0 = no limit) */
  maxLength?: number;
  /** Input name attribute */
  name?: string;
  /** Custom inline styles */
  style?: React.CSSProperties;
  /** Border radius */
  borderRadius?: string;
  /** Border style */
  border?: string;
  /** Font size */
  fontSize?: string;
  /** Background color */
  backgroundColor?: string;
  /** Text color */
  color?: string;
  /** Padding */
  padding?: string;
  /** Edit mode flag */
  isEditMode?: boolean;
  /** Component instance (optional for standalone usage) */
  component?: ComponentInstance;
  /** Change handler */
  onChange?: (e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement>) => void;
}

/**
 * TextboxRenderer - Renders a text input or textarea component
 * Supports single-line input and multiline textarea modes
 */
const TextboxRenderer: React.FC<TextboxProps> = (props) => {
  const [value, setValue] = useState('');
  const [isFocused, setIsFocused] = useState(false);

  const { component, isEditMode } = props;
  const cp = component?.props;

  const label = (props.label ?? cp?.label as string) ?? '';
  const showLabel = (props.showLabel ?? cp?.showLabel as boolean) ?? false;
  const placeholder = (props.placeholder ?? cp?.placeholder as string) ?? 'Enter text...';
  const type = (props.type ?? cp?.type as string) ?? 'text';
  const multiline = (props.multiline ?? cp?.multiline as boolean) ?? false;
  const rows = (props.rows ?? cp?.rows as number) ?? 3;
  const disabled = (props.disabled ?? cp?.disabled as boolean) ?? false;
  const readOnly = (props.readOnly ?? cp?.readOnly as boolean) ?? false;
  const required = (props.required ?? cp?.required as boolean) ?? false;
  const maxLength = (props.maxLength ?? cp?.maxLength as number) ?? 0;
  const name = (props.name ?? cp?.name as string) ?? '';

  // Get custom styles: direct props override component.styles
  const customStyles: React.CSSProperties = {
    ...(component?.styles as React.CSSProperties),
    borderRadius: props.borderRadius,
    border: props.border,
    fontSize: props.fontSize,
    backgroundColor: props.backgroundColor,
    color: props.color,
    padding: props.padding,
    ...props.style,
  };

  // Build the style object
  const inputStyles: React.CSSProperties = {
    width: '100%',
    boxSizing: 'border-box',
    borderRadius: '4px',
    border: '1px solid #ccc',
    padding: '8px 12px',
    fontSize: '14px',
    backgroundColor: '#ffffff',
    color: '#333333',
    outline: 'none',
    transition: 'border-color 0.2s, box-shadow 0.2s',
    resize: multiline ? 'vertical' : 'none',
    fontFamily: 'inherit',
    ...Object.fromEntries(
      Object.entries(customStyles).filter(([, value]) => value !== undefined)
    ),
  };

  // Focus styles
  if (isFocused) {
    inputStyles.borderColor = '#007bff';
    inputStyles.boxShadow = '0 0 0 2px rgba(0, 123, 255, 0.25)';
  }

  // Disabled styles
  if (disabled) {
    inputStyles.backgroundColor = '#f5f5f5';
    inputStyles.cursor = 'not-allowed';
    inputStyles.opacity = 0.7;
  }

  // Label styles
  const labelStyles: React.CSSProperties = {
    display: 'block',
    marginBottom: '4px',
    fontSize: '14px',
    fontWeight: 500,
    color: '#333333',
  };

  // Container styles
  const containerStyles: React.CSSProperties = {
    width: '100%',
  };

  // Handle input change
  const handleChange = (e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement>) => {
    if (!isEditMode) {
      setValue(e.target.value);
      if (props.onChange) {
        props.onChange(e);
      }
    }
  };

  // Handle focus
  const handleFocus = () => {
    setIsFocused(true);
  };

  // Handle blur
  const handleBlur = () => {
    setIsFocused(false);
  };

  // Common input props
  const inputProps = {
    placeholder: placeholder as string,
    disabled: (disabled as boolean) || isEditMode,
    readOnly: readOnly as boolean,
    required: required as boolean,
    name: name as string,
    value: isEditMode ? '' : value,
    onChange: handleChange,
    onFocus: handleFocus,
    onBlur: handleBlur,
    style: inputStyles,
    className: 'textbox-input',
    ...((maxLength as number) > 0 && { maxLength: maxLength as number }),
  };

  return (
    <div style={containerStyles} className="textbox-renderer">
      {showLabel && label && (
        <label style={labelStyles} className="textbox-label">
          {label as string}
          {required && <span style={{ color: '#dc3545', marginLeft: '2px' }}>*</span>}
        </label>
      )}
      {multiline ? (
        <textarea {...inputProps} rows={rows as number} />
      ) : (
        <input {...inputProps} type={type as string} />
      )}
    </div>
  );
};

export default TextboxRenderer;
export { TextboxRenderer };
