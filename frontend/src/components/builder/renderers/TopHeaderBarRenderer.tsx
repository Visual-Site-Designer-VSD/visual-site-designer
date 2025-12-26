import React, { useMemo } from 'react';
import { RendererProps } from './RendererRegistry';
import { useComponentEvents } from '../events';

/**
 * SocialLink interface for social media links
 */
interface SocialLink {
  platform: string;
  url: string;
  icon: string;
}

/**
 * TopHeaderBarRenderer - Utility bar component for above main navigation
 * Features:
 * - Left/Center/Right content areas
 * - Social media links
 * - Contact information display
 * - Announcements/promotions
 *
 * Category: navbar
 */
// Default social links
const defaultSocialLinks = [
  { platform: 'facebook', url: '#', icon: '📘' },
  { platform: 'twitter', url: '#', icon: '🐦' },
  { platform: 'linkedin', url: '#', icon: '💼' },
];

// Helper to check if socialLinks has actual content
const hasSocialLinks = (items: unknown): boolean => {
  if (!items) return false;
  if (Array.isArray(items) && items.length > 0) return true;
  if (typeof items === 'string' && items.trim() !== '' && items !== '[]') return true;
  return false;
};

const TopHeaderBarRenderer: React.FC<RendererProps> = ({ component, isEditMode }) => {
  // Get event handlers from the event system
  const eventHandlers = useComponentEvents(component, { isEditMode });

  // Extract props with defaults
  const {
    leftContent = '📧 contact@example.com',
    rightContent = '📞 +1 234 567 890',
    centerContent = '',
    showSocialLinks = true,
    socialLinks: propsSocialLinks,
  } = component.props;

  // Use component's socialLinks if they exist, otherwise use defaults
  const socialLinks = hasSocialLinks(propsSocialLinks) ? propsSocialLinks : defaultSocialLinks;

  // Extract styles with defaults
  const {
    backgroundColor = '#f8f9fa',
    textColor = '#666666',
    accentColor = '#007bff',
    padding = '8px 20px',
    borderBottom = '1px solid #e9ecef',
    fontFamily = 'inherit',
    fontSize = '13px',
  } = component.styles as Record<string, string>;

  // Parse socialLinks if it's a string (JSON)
  const parsedSocialLinks: SocialLink[] = useMemo(() => {
    if (typeof socialLinks === 'string') {
      try {
        return JSON.parse(socialLinks);
      } catch (e) {
        console.warn('Failed to parse socialLinks:', e);
        return [];
      }
    }
    return Array.isArray(socialLinks) ? socialLinks : [];
  }, [socialLinks]);

  // Container styles
  const containerStyles: React.CSSProperties = {
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'space-between',
    width: '100%',
    backgroundColor,
    color: textColor,
    padding,
    borderBottom,
    fontFamily,
    fontSize,
    boxSizing: 'border-box',
  };

  // Section styles (left, center, right)
  const sectionStyles: React.CSSProperties = {
    display: 'flex',
    alignItems: 'center',
    gap: '16px',
  };

  const leftSectionStyles: React.CSSProperties = {
    ...sectionStyles,
    justifyContent: 'flex-start',
    flex: 1,
  };

  const centerSectionStyles: React.CSSProperties = {
    ...sectionStyles,
    justifyContent: 'center',
    flex: centerContent ? 2 : 0,
  };

  const rightSectionStyles: React.CSSProperties = {
    ...sectionStyles,
    justifyContent: 'flex-end',
    flex: 1,
  };

  // Content text styles
  const contentTextStyles: React.CSSProperties = {
    margin: 0,
    whiteSpace: 'nowrap',
  };

  // Social links container styles
  const socialContainerStyles: React.CSSProperties = {
    display: 'flex',
    alignItems: 'center',
    gap: '8px',
  };

  // Social link styles
  const socialLinkStyles: React.CSSProperties = {
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    width: '24px',
    height: '24px',
    textDecoration: 'none',
    color: textColor,
    transition: 'color 0.2s ease',
    cursor: isEditMode ? 'default' : 'pointer',
  };

  // Link styles
  const linkStyles: React.CSSProperties = {
    color: accentColor,
    textDecoration: 'none',
    transition: 'opacity 0.2s ease',
    cursor: isEditMode ? 'default' : 'pointer',
  };

  // Handle link click in edit mode
  const handleClick = (e: React.MouseEvent) => {
    if (isEditMode) {
      e.preventDefault();
    }
    if (eventHandlers.onClick) {
      eventHandlers.onClick(e);
    }
  };

  // Render content with link detection
  const renderContent = (content: string | unknown) => {
    if (!content) return null;
    const text = String(content);

    // Simple pattern matching for common contact formats
    const emailRegex = /([a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,})/g;
    const phoneRegex = /(\+?[\d\s-()]{10,})/g;

    // For simplicity, just render as text (links can be added later if needed)
    return <span style={contentTextStyles}>{text}</span>;
  };

  // Render social link
  const renderSocialLink = (link: SocialLink, index: number) => (
    <a
      key={`${link.platform}-${index}`}
      href={link.url}
      style={socialLinkStyles}
      onClick={handleClick}
      onMouseEnter={(e) => {
        if (!isEditMode) {
          (e.currentTarget as HTMLElement).style.color = accentColor;
        }
      }}
      onMouseLeave={(e) => {
        if (!isEditMode) {
          (e.currentTarget as HTMLElement).style.color = textColor;
        }
      }}
      title={link.platform}
      target="_blank"
      rel="noopener noreferrer"
    >
      {link.icon || getPlatformIcon(link.platform)}
    </a>
  );

  // Get default icon for known platforms
  const getPlatformIcon = (platform: string): string => {
    const icons: Record<string, string> = {
      facebook: '📘',
      twitter: '🐦',
      instagram: '📷',
      linkedin: '💼',
      youtube: '📺',
      github: '💻',
      email: '✉️',
      phone: '📞',
    };
    return icons[platform.toLowerCase()] || '🔗';
  };

  // Check if there's any content to show
  const hasContent = leftContent || rightContent || centerContent || (showSocialLinks && parsedSocialLinks.length > 0);

  // Empty state for edit mode
  if (!hasContent && isEditMode) {
    return (
      <div style={{
        ...containerStyles,
        justifyContent: 'center',
        color: '#999',
        fontStyle: 'italic',
      }}>
        Add content in Properties panel (left/right/center content or social links)
      </div>
    );
  }

  return (
    <div style={containerStyles}>
      {/* Left section */}
      <div style={leftSectionStyles}>
        {renderContent(leftContent)}
      </div>

      {/* Center section */}
      {centerContent && (
        <div style={centerSectionStyles}>
          {renderContent(centerContent)}
        </div>
      )}

      {/* Right section */}
      <div style={rightSectionStyles}>
        {renderContent(rightContent)}

        {/* Social links */}
        {showSocialLinks && parsedSocialLinks.length > 0 && (
          <div style={socialContainerStyles}>
            {parsedSocialLinks.map((link, index) => renderSocialLink(link, index))}
          </div>
        )}
      </div>
    </div>
  );
};

export default TopHeaderBarRenderer;
export { TopHeaderBarRenderer };
