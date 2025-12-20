import React, { useState } from 'react';

/**
 * Textbox Component
 * Renders a text input or textarea for user input
 */
const Textbox = ({ component, isEditMode }) => {
  const [value, setValue] = useState('');
  const [isFocused, setIsFocused] = useState(false);

  // Extract props with defaults
  const {
    placeholder = 'Enter text...',
    type = 'text',
    disabled = false,
    readOnly = false,
    required = false,
    multiline = false,
    rows = 3,
    maxLength = 0,
    name = '',
    label = '',
    showLabel = false,
  } = component.props || {};

  // Get styles from component
  const customStyles = component.styles || {};

  // Build the style object
  const inputStyles = {
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
    ...customStyles,
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
  const labelStyles = {
    display: 'block',
    marginBottom: '4px',
    fontSize: '14px',
    fontWeight: '500',
    color: '#333333',
  };

  // Container styles
  const containerStyles = {
    width: '100%',
  };

  // Handle input change
  const handleChange = (e) => {
    if (!isEditMode) {
      setValue(e.target.value);
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
    placeholder,
    disabled: disabled || isEditMode,
    readOnly,
    required,
    name,
    value: isEditMode ? '' : value,
    onChange: handleChange,
    onFocus: handleFocus,
    onBlur: handleBlur,
    style: inputStyles,
    className: 'textbox-input',
    ...(maxLength > 0 && { maxLength }),
  };

  return (
    <div style={containerStyles} className="textbox-component">
      {showLabel && label && (
        <label style={labelStyles} className="textbox-label">
          {label}
          {required && <span style={{ color: '#dc3545', marginLeft: '2px' }}>*</span>}
        </label>
      )}
      {multiline ? (
        <textarea {...inputProps} rows={rows} />
      ) : (
        <input {...inputProps} type={type} />
      )}
    </div>
  );
};

export default Textbox;
export { Textbox };
