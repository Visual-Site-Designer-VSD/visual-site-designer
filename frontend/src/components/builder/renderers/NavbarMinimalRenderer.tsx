import React from 'react';
import { RendererProps } from './RendererRegistry';
import NavbarRenderer from './NavbarRenderer';

/**
 * NavbarMinimalRenderer - Minimal clean navbar layout
 * Simple, clean design with minimal styling
 *
 * This is a variant that wraps NavbarRenderer with specific default props
 * Category: navbar
 */
// Default navigation items for this variant (minimal/clean)
const defaultNavItems = [
  { label: 'Home', href: '/', active: true },
  { label: 'Work', href: '/work', active: false },
  { label: 'Contact', href: '/contact', active: false },
];

// Helper to check if navItems has actual content
const hasNavItems = (items: unknown): boolean => {
  if (!items) return false;
  if (Array.isArray(items) && items.length > 0) return true;
  if (typeof items === 'string' && items.trim() !== '' && items !== '[]') return true;
  return false;
};

const NavbarMinimalRenderer: React.FC<RendererProps> = ({ component, isEditMode }) => {
  // Use component's navItems if they exist, otherwise use defaults
  const navItems = hasNavItems(component.props?.navItems)
    ? component.props.navItems
    : defaultNavItems;

  // Merge default props for this variant
  const enhancedComponent = {
    ...component,
    props: {
      layout: 'minimal',
      brandText: 'Brand',
      ...component.props,
      navItems, // Override with resolved navItems
    },
    styles: {
      backgroundColor: 'transparent',
      textColor: '#333333',
      accentColor: '#000000',
      padding: '0 16px',
      boxShadow: 'none',
      borderBottom: 'none',
      ...component.styles,
    },
  };

  return <NavbarRenderer component={enhancedComponent} isEditMode={isEditMode} />;
};

export default NavbarMinimalRenderer;
export { NavbarMinimalRenderer };
