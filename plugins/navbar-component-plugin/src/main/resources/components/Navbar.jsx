import React, { useState, useEffect } from 'react';

/**
 * Navbar Component
 * A customizable navigation bar with responsive design
 *
 * Props:
 * - brandText: Text for the brand/logo
 * - brandImageUrl: URL for the brand logo image
 * - brandLink: URL the brand links to
 * - navItems: Array of navigation items [{label, href, active}]
 * - layout: Layout variant (default, centered, split, minimal)
 * - sticky: Whether navbar is sticky/fixed
 * - showMobileMenu: Show hamburger menu on mobile
 * - mobileBreakpoint: Screen width for mobile menu
 * - styles: Custom styles object
 * - onClick: Click handler for nav items
 */
const Navbar = ({
  brandText = 'My Site',
  brandImageUrl = '',
  brandLink = '/',
  navItems = [],
  layout = 'default',
  sticky = false,
  showMobileMenu = true,
  mobileBreakpoint = '768px',
  styles = {},
  onClick,
  isEditMode = false,
}) => {
  const [isMobileMenuOpen, setIsMobileMenuOpen] = useState(false);
  const [isMobile, setIsMobile] = useState(false);

  // Parse navItems if it's a string (JSON)
  const parsedNavItems = React.useMemo(() => {
    if (typeof navItems === 'string') {
      try {
        return JSON.parse(navItems);
      } catch (e) {
        console.warn('Failed to parse navItems:', e);
        return [];
      }
    }
    return Array.isArray(navItems) ? navItems : [];
  }, [navItems]);

  // Check for mobile viewport
  useEffect(() => {
    const checkMobile = () => {
      const breakpoint = parseInt(mobileBreakpoint) || 768;
      setIsMobile(window.innerWidth < breakpoint);
    };

    checkMobile();
    window.addEventListener('resize', checkMobile);
    return () => window.removeEventListener('resize', checkMobile);
  }, [mobileBreakpoint]);

  // Close mobile menu when switching to desktop
  useEffect(() => {
    if (!isMobile) {
      setIsMobileMenuOpen(false);
    }
  }, [isMobile]);

  // Extract style props
  const {
    backgroundColor = '#ffffff',
    textColor = '#333333',
    accentColor = '#007bff',
    padding = '0 20px',
    boxShadow = '0 2px 4px rgba(0,0,0,0.1)',
    borderBottom = '1px solid #e0e0e0',
    fontFamily = 'inherit',
    fontSize = '16px',
    ...restStyles
  } = styles;

  // Container styles
  const containerStyles = {
    display: 'flex',
    alignItems: 'center',
    justifyContent: getJustifyContent(layout),
    width: '100%',
    height: '100%',
    minHeight: '40px',
    backgroundColor,
    color: textColor,
    padding,
    boxShadow,
    borderBottom,
    fontFamily,
    fontSize,
    boxSizing: 'border-box',
    position: sticky ? 'sticky' : 'relative',
    top: sticky ? 0 : 'auto',
    zIndex: sticky ? 1000 : 'auto',
    transition: 'all 0.3s ease',
    ...restStyles,
  };

  // Brand styles
  const brandStyles = {
    display: 'flex',
    alignItems: 'center',
    gap: '10px',
    textDecoration: 'none',
    color: textColor,
    fontWeight: 600,
    fontSize: '1.25em',
    cursor: isEditMode ? 'default' : 'pointer',
  };

  // Nav list styles
  const navListStyles = {
    display: isMobile && showMobileMenu ? (isMobileMenuOpen ? 'flex' : 'none') : 'flex',
    flexDirection: isMobile ? 'column' : 'row',
    alignItems: isMobile ? 'stretch' : 'center',
    gap: isMobile ? '0' : '24px',
    listStyle: 'none',
    margin: 0,
    padding: isMobile ? '10px 0' : 0,
    position: isMobile ? 'absolute' : 'static',
    top: isMobile ? '100%' : 'auto',
    left: 0,
    right: 0,
    backgroundColor: isMobile ? backgroundColor : 'transparent',
    boxShadow: isMobile && isMobileMenuOpen ? '0 4px 6px rgba(0,0,0,0.1)' : 'none',
    zIndex: 999,
  };

  // Nav link styles
  const getLinkStyles = (item) => ({
    display: 'block',
    padding: isMobile ? '12px 20px' : '8px 0',
    textDecoration: 'none',
    color: item.active ? accentColor : textColor,
    fontWeight: item.active ? 600 : 400,
    borderBottom: !isMobile && item.active ? `2px solid ${accentColor}` : '2px solid transparent',
    transition: 'all 0.2s ease',
    cursor: isEditMode ? 'default' : 'pointer',
  });

  // Hamburger button styles
  const hamburgerStyles = {
    display: isMobile && showMobileMenu ? 'flex' : 'none',
    flexDirection: 'column',
    justifyContent: 'space-around',
    width: '24px',
    height: '20px',
    background: 'transparent',
    border: 'none',
    cursor: 'pointer',
    padding: 0,
  };

  const hamburgerLineStyles = {
    width: '24px',
    height: '3px',
    backgroundColor: textColor,
    borderRadius: '2px',
    transition: 'all 0.3s ease',
  };

  // Handle nav item click
  const handleNavClick = (e, item) => {
    if (isEditMode) {
      e.preventDefault();
      return;
    }
    if (onClick) {
      onClick(e, item);
    }
    if (isMobile) {
      setIsMobileMenuOpen(false);
    }
  };

  // Handle brand click
  const handleBrandClick = (e) => {
    if (isEditMode) {
      e.preventDefault();
    }
  };

  return (
    <nav style={containerStyles}>
      {/* Brand/Logo */}
      <a href={brandLink} style={brandStyles} onClick={handleBrandClick}>
        {brandImageUrl && (
          <img
            src={brandImageUrl}
            alt={brandText || 'Logo'}
            style={{ height: '32px', width: 'auto' }}
          />
        )}
        {brandText && <span>{brandText}</span>}
      </a>

      {/* Spacer for split layout */}
      {layout === 'split' && <div style={{ flex: 1 }} />}

      {/* Navigation Links */}
      {parsedNavItems.length > 0 && (
        <ul style={navListStyles}>
          {parsedNavItems.map((item, index) => (
            <li key={index}>
              <a
                href={item.href || '#'}
                style={getLinkStyles(item)}
                onClick={(e) => handleNavClick(e, item)}
                onMouseEnter={(e) => {
                  if (!isEditMode) {
                    e.target.style.color = accentColor;
                  }
                }}
                onMouseLeave={(e) => {
                  if (!isEditMode) {
                    e.target.style.color = item.active ? accentColor : textColor;
                  }
                }}
              >
                {item.label}
              </a>
            </li>
          ))}
        </ul>
      )}

      {/* Mobile Menu Button */}
      {showMobileMenu && (
        <button
          style={hamburgerStyles}
          onClick={() => setIsMobileMenuOpen(!isMobileMenuOpen)}
          aria-label="Toggle navigation menu"
          aria-expanded={isMobileMenuOpen}
        >
          <span
            style={{
              ...hamburgerLineStyles,
              transform: isMobileMenuOpen ? 'rotate(45deg) translate(5px, 5px)' : 'none',
            }}
          />
          <span
            style={{
              ...hamburgerLineStyles,
              opacity: isMobileMenuOpen ? 0 : 1,
            }}
          />
          <span
            style={{
              ...hamburgerLineStyles,
              transform: isMobileMenuOpen ? 'rotate(-45deg) translate(5px, -5px)' : 'none',
            }}
          />
        </button>
      )}

      {/* Empty state for edit mode */}
      {parsedNavItems.length === 0 && isEditMode && (
        <span style={{ color: '#999', fontStyle: 'italic', marginLeft: 'auto' }}>
          Add navigation items in Properties panel
        </span>
      )}
    </nav>
  );
};

/**
 * Get justify-content value based on layout
 */
function getJustifyContent(layout) {
  switch (layout) {
    case 'centered':
      return 'center';
    case 'split':
      return 'space-between';
    case 'minimal':
      return 'flex-start';
    case 'default':
    default:
      return 'space-between';
  }
}

export default Navbar;
