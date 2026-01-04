import React, { useState } from 'react';
import type { ComponentInstance } from '../types';

// Cross-plugin imports using @vsd aliases - import renderers with direct props support
import { LabelRenderer } from '@vsd/label-component-plugin';
import { ButtonRenderer } from '@vsd/button-component-plugin';
import { TextboxRenderer } from '@vsd/textbox-component-plugin';

/**
 * Props for the NewsletterForm component.
 * Use these props directly: <NewsletterFormRenderer title="Subscribe" buttonText="Join" />
 */
export interface NewsletterFormProps {
  /** Form title text */
  title?: string;
  /** Form subtitle/description text */
  subtitle?: string;
  /** Email input placeholder text */
  placeholder?: string;
  /** Submit button text */
  buttonText?: string;
  /** Button variant style */
  buttonVariant?: 'primary' | 'secondary' | 'success' | 'danger' | 'warning' | 'outline' | 'link';
  /** Form layout: stacked or inline */
  layout?: 'stacked' | 'inline';
  /** Show title */
  showTitle?: boolean;
  /** Show subtitle */
  showSubtitle?: boolean;
  /** Success message after subscription */
  successMessage?: string;
  /** Error message for invalid email */
  errorMessage?: string;
  /** Edit mode flag */
  isEditMode?: boolean;
  /** Custom inline styles */
  style?: React.CSSProperties;
  /** Title color */
  titleColor?: string;
  /** Subtitle color */
  subtitleColor?: string;
  /** Background color */
  backgroundColor?: string;
  /** Padding */
  padding?: string;
  /** Border radius */
  borderRadius?: string;
  /** @deprecated Use direct props instead. Legacy component prop for backward compatibility */
  component?: ComponentInstance;
}

/**
 * NewsletterFormRenderer - A composite component that renders a newsletter subscription form
 * using Label and Button component renderers from other plugins.
 *
 * This component demonstrates cross-plugin composition with simple direct props:
 *   <LabelRenderer text="Subscribe" variant="h3" textAlign="center" />
 *   <ButtonRenderer text="Join" variant="primary" />
 */
const NewsletterFormRenderer: React.FC<NewsletterFormProps> = (props) => {
  const [email, setEmail] = useState('');
  const [isSubmitted, setIsSubmitted] = useState(false);
  const [hasError, setHasError] = useState(false);

  // Support both new direct props and legacy component prop
  const isLegacyMode = props.component !== undefined;

  const {
    title = isLegacyMode ? (props.component?.props?.title as string) ?? 'Subscribe to Our Newsletter' : 'Subscribe to Our Newsletter',
    subtitle = isLegacyMode ? (props.component?.props?.subtitle as string) ?? 'Get the latest updates delivered to your inbox.' : 'Get the latest updates delivered to your inbox.',
    placeholder = isLegacyMode ? (props.component?.props?.placeholder as string) ?? 'Enter your email address' : 'Enter your email address',
    buttonText = isLegacyMode ? (props.component?.props?.buttonText as string) ?? 'Subscribe' : 'Subscribe',
    buttonVariant = isLegacyMode ? (props.component?.props?.buttonVariant as string) ?? 'primary' : 'primary',
    layout = isLegacyMode ? (props.component?.props?.layout as string) ?? 'stacked' : 'stacked',
    showTitle = isLegacyMode ? (props.component?.props?.showTitle as boolean) ?? true : true,
    showSubtitle = isLegacyMode ? (props.component?.props?.showSubtitle as boolean) ?? true : true,
    successMessage = isLegacyMode ? (props.component?.props?.successMessage as string) ?? 'Thank you for subscribing!' : 'Thank you for subscribing!',
    errorMessage = isLegacyMode ? (props.component?.props?.errorMessage as string) ?? 'Please enter a valid email address.' : 'Please enter a valid email address.',
    isEditMode = false,
  } = props;

  // Get custom styles from either direct props or legacy component.styles
  const customStyles: React.CSSProperties = isLegacyMode
    ? (props.component?.styles as React.CSSProperties) || {}
    : {
        backgroundColor: props.backgroundColor,
        padding: props.padding,
        borderRadius: props.borderRadius,
        ...props.style,
      };

  const titleColor = isLegacyMode
    ? (props.component?.styles?.titleColor as string) || '#333333'
    : props.titleColor || '#333333';

  const subtitleColor = isLegacyMode
    ? (props.component?.styles?.subtitleColor as string) || '#666666'
    : props.subtitleColor || '#666666';

  // Email validation
  const validateEmail = (emailValue: string): boolean => {
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    return emailRegex.test(emailValue);
  };

  // Handle form submission
  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (isEditMode) return;

    if (validateEmail(email)) {
      setIsSubmitted(true);
      setHasError(false);
      console.log('Newsletter subscription:', email);
    } else {
      setHasError(true);
    }
  };

  // Handle input change
  const handleInputChange = (e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement>) => {
    setEmail(e.target.value);
    if (hasError) setHasError(false);
    if (isSubmitted) setIsSubmitted(false);
  };

  // Container styles
  const containerStyles: React.CSSProperties = {
    backgroundColor: (customStyles.backgroundColor as string) || '#f8f9fa',
    padding: (customStyles.padding as string) || '24px',
    borderRadius: (customStyles.borderRadius as string) || '8px',
    textAlign: 'center',
    width: '100%',
    boxSizing: 'border-box',
  };

  // Form row styles for inline layout
  const formRowStyles: React.CSSProperties = {
    display: layout === 'inline' ? 'flex' : 'block',
    alignItems: 'center',
    justifyContent: 'center',
    gap: layout === 'inline' ? '10px' : 0,
  };

  // Message styles
  const successStyles: React.CSSProperties = {
    marginTop: '12px',
    fontSize: '14px',
    fontWeight: 500,
    color: '#28a745',
  };

  const errorStyles: React.CSSProperties = {
    marginTop: '12px',
    fontSize: '14px',
    fontWeight: 500,
    color: '#dc3545',
  };

  // If submitted, show success message
  if (isSubmitted && !isEditMode) {
    return (
      <div style={containerStyles}>
        <div style={successStyles}>{String(successMessage)}</div>
      </div>
    );
  }

  return (
    <div style={containerStyles}>
      {/* Title - Using LabelRenderer with direct props */}
      {Boolean(showTitle) && (
        <LabelRenderer
          text={title as string}
          variant="h3"
          textAlign="center"
          color={titleColor}
          isEditMode={isEditMode}
        />
      )}

      {/* Subtitle - Using LabelRenderer with direct props */}
      {Boolean(showTitle) && Boolean(showSubtitle) && (
        <LabelRenderer
          text={subtitle as string}
          variant="p"

          textAlign="center"
          color={subtitleColor}
          isEditMode={isEditMode}
        />
      )}

      {/* Form */}
      <form onSubmit={handleSubmit} style={{ margin: 0 }}>
        <div style={formRowStyles}>
          {/* Email Input - Using TextboxRenderer with direct props */}
          <div style={{ width: layout === 'inline' ? '70%' : '100%' }}>
            <TextboxRenderer
              type="email"
              placeholder={String(placeholder)}
              onChange={handleInputChange}
              isEditMode={isEditMode}
              border={hasError ? '1px solid #dc3545' : undefined}
              style={{ width: '100%' }}
            />
          </div>

          {/* Submit Button - Using ButtonRenderer with direct props */}
          <div
            style={layout === 'inline' ? {} : { marginTop: '12px' }}
            onClick={!isEditMode ? handleSubmit : undefined}
          >
            <ButtonRenderer
              text={buttonText as string}
              variant={buttonVariant as 'primary' | 'secondary' | 'success' | 'danger' | 'warning' | 'outline' | 'link'}
              size="medium"
              fullWidth={layout !== 'inline'}
              isEditMode={isEditMode}
            />
          </div>
        </div>

        {/* Error Message */}
        {hasError && !isEditMode && (
          <div style={errorStyles}>{String(errorMessage)}</div>
        )}
      </form>
    </div>
  );
};

export default NewsletterFormRenderer;
export { NewsletterFormRenderer };
