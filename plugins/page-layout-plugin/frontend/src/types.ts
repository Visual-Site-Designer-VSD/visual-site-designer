/**
 * Type definitions for Page Layout plugin renderers.
 */

export interface ComponentPosition {
  x: number;
  y: number;
}

export interface ComponentSize {
  width: number;
  height: number;
}

export interface ComponentInstance {
  instanceId: string;
  pluginId: string;
  componentId: string;
  componentCategory?: string;
  parentId?: string | null;
  position: ComponentPosition;
  size: ComponentSize;
  props: Record<string, unknown>;
  styles: Record<string, string>;
  children?: ComponentInstance[];
  zIndex?: number;
  displayOrder?: number;
  isVisible?: boolean;
  reactBundlePath?: string;
}

export interface RendererProps {
  component: ComponentInstance;
  isEditMode: boolean;
}

export type RendererComponent = React.FC<RendererProps>;

export interface PluginBundle {
  pluginId: string;
  renderers: Record<string, RendererComponent>;
  styles?: string;
  version?: string;
}

// Slot types for Page Layout
export type PageLayoutSlot = 'header' | 'footer' | 'left' | 'right' | 'center';

export const PAGE_LAYOUT_SLOTS: PageLayoutSlot[] = ['header', 'footer', 'left', 'right', 'center'];

/**
 * Slot visibility configuration per breakpoint
 */
export interface SlotVisibility {
  header: boolean;
  footer: boolean;
  left: boolean;
  right: boolean;
  center: boolean;
}

/**
 * Breakpoint-specific settings for responsive layout
 */
export interface BreakpointSettings {
  slotVisibility: SlotVisibility;
  slotStackingOrder: PageLayoutSlot[];
  sidebarRatio?: string;
  stackSidebars?: boolean;
}

/**
 * Responsive configuration for all breakpoints
 */
export interface ResponsiveConfig {
  mobile: BreakpointSettings;
  tablet: BreakpointSettings;
  desktop: BreakpointSettings;
  large: BreakpointSettings;
}

/**
 * Page Layout specific props
 */
export interface PageLayoutProps {
  headerHeight?: string;
  footerHeight?: string;
  leftWidth?: string;
  rightWidth?: string;
  gap?: string;
  padding?: string;
  fullHeight?: boolean;
  stickyHeader?: boolean;
  stickyFooter?: boolean;
  sidebarRatio?: string;  // e.g., '30-70' for 30% left, 70% center
  availableSlots?: PageLayoutSlot[];
  responsive?: ResponsiveConfig;  // Responsive settings per breakpoint
}

/**
 * Page Layout styles (region-specific backgrounds)
 */
export interface PageLayoutStyles {
  backgroundColor?: string;
  headerBackground?: string;
  footerBackground?: string;
  sidebarBackground?: string;
  centerBackground?: string;
}

/**
 * Helper to get slot from component props
 */
export function getComponentSlot(component: ComponentInstance): PageLayoutSlot {
  const slot = component.props?.slot as PageLayoutSlot | undefined;
  if (slot && PAGE_LAYOUT_SLOTS.includes(slot)) {
    return slot;
  }
  return 'center'; // Default to center if not specified
}

/**
 * Group children by their assigned slot
 */
export function groupChildrenBySlot(children: ComponentInstance[]): Record<PageLayoutSlot, ComponentInstance[]> {
  const grouped: Record<PageLayoutSlot, ComponentInstance[]> = {
    header: [],
    footer: [],
    left: [],
    right: [],
    center: [],
  };

  children.forEach(child => {
    const slot = getComponentSlot(child);
    grouped[slot].push(child);
  });

  return grouped;
}
