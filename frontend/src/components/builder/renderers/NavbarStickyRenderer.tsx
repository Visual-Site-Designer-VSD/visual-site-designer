import React from 'react';
import { RendererProps } from './RendererRegistry';
import NavbarRenderer from './NavbarRenderer';

/**
 * NavbarStickyRenderer - Always visible sticky header navbar
 * Fixed to the top of the viewport when scrolling
 *
 * This is a variant that wraps NavbarRenderer with specific default props
 * Category: navbar
 */
// Default navigation items for this variant
const defaultNavItems = [
  { label: 'Home', href: '/', active: true },
  { label: 'About', href: '/about', active: false },
  { label: 'Services', href: '/services', active: false },
  { label: 'Contact', href: '/contact', active: false },
];

// Helper to check if navItems has actual content
const hasNavItems = (items: unknown): boolean => {
  if (!items) return false;
  if (Array.isArray(items) && items.length > 0) return true;
  if (typeof items === 'string' && items.trim() !== '' && items !== '[]') return true;
  return false;
};

const NavbarStickyRenderer: React.FC<RendererProps> = ({ component, isEditMode }) => {
  // Use component's navItems if they exist, otherwise use defaults
  const navItems = hasNavItems(component.props?.navItems)
    ? component.props.navItems
    : defaultNavItems;

  // Merge default props for this variant
  const enhancedComponent = {
    ...component,
    props: {
      layout: 'default',
      brandText: 'My Site',
      sticky: true, // Key difference - always sticky
      ...component.props,
      navItems, // Override with resolved navItems
    },
    styles: {
      backgroundColor: '#ffffff',
      textColor: '#333333',
      accentColor: '#007bff',
      padding: '0 20px',
      boxShadow: '0 2px 10px rgba(0,0,0,0.15)',
      borderBottom: 'none',
      ...component.styles,
    },
  };

  return <NavbarRenderer component={enhancedComponent} isEditMode={isEditMode} />;
};

export default NavbarStickyRenderer;
export { NavbarStickyRenderer };
