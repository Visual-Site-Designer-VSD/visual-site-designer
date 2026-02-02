import React, { useMemo, useState, ReactNode } from 'react';
import type { RendererProps, PageLayoutProps, PageLayoutStyles, PageLayoutSlot, ComponentInstance, ResponsiveConfig, BreakpointSettings, MobileSidebarBehavior } from '../types';
import { groupChildrenBySlot } from '../types';

// Breakpoint definitions (matching the main app's responsive.ts)
const BREAKPOINTS = {
  mobile: { minWidth: 0, maxWidth: 575 },
  tablet: { minWidth: 576, maxWidth: 991 },
  desktop: { minWidth: 992, maxWidth: 1199 },
  large: { minWidth: 1200, maxWidth: null },
};

type BreakpointName = 'mobile' | 'tablet' | 'desktop' | 'large';

// Default responsive configuration
// Note: sidebarRatio is intentionally NOT set here for most breakpoints
// so that the component's sidebarRatio prop is used as the default.
// Only set sidebarRatio when you want to OVERRIDE the component prop for a specific breakpoint.
const DEFAULT_RESPONSIVE_CONFIG: ResponsiveConfig = {
  mobile: {
    slotVisibility: { header: true, footer: true, left: false, right: false, center: true },
    slotStackingOrder: ['header', 'center', 'footer'],
    stackSidebars: true,
    // No sidebarRatio - sidebars are stacked anyway
  },
  tablet: {
    slotVisibility: { header: true, footer: true, left: true, right: false, center: true },
    slotStackingOrder: ['header', 'left', 'center', 'footer'],
    // No sidebarRatio - use component's sidebarRatio prop
    stackSidebars: false,
  },
  desktop: {
    slotVisibility: { header: true, footer: true, left: true, right: false, center: true },
    slotStackingOrder: ['header', 'left', 'center', 'footer'],
    // No sidebarRatio - use component's sidebarRatio prop
    stackSidebars: false,
  },
  large: {
    slotVisibility: { header: true, footer: true, left: true, right: true, center: true },
    slotStackingOrder: ['header', 'left', 'center', 'right', 'footer'],
    // No sidebarRatio - use component's sidebarRatio prop
    stackSidebars: false,
  },
};

/**
 * PageLayoutRenderer - Renders a structured page layout with visual wireframe regions
 *
 * In Edit Mode: Shows a visual wireframe with labeled regions (Header, Left Panel, Content, Footer)
 * In Preview Mode: Renders the actual layout with intelligent region expansion
 */

// Access global RendererRegistry exposed by the main app
// This allows us to render children without requiring a renderChild prop
interface RendererRegistry {
  get: (componentId: string, pluginId?: string) => React.FC<RendererProps> | null;
}

const getGlobalRendererRegistry = (): RendererRegistry | null => {
  return (globalThis as unknown as { RendererRegistry?: RendererRegistry }).RendererRegistry || null;
};

/**
 * SlotChildRenderer - Renders a child component using the global RendererRegistry
 * Plugins are expected to be preloaded by MultiPagePreview before rendering
 */
interface SlotChildRendererProps {
  child: ComponentInstance;
  isEditMode: boolean;
  isAutoHeightSlot: boolean;
  childSizeStyles: React.CSSProperties;
}

const SlotChildRenderer: React.FC<SlotChildRendererProps> = ({
  child,
  isEditMode,
  isAutoHeightSlot,
  childSizeStyles,
}) => {
  const registry = getGlobalRendererRegistry();
  const ChildRenderer = registry?.get(child.componentId, child.pluginId);

  if (ChildRenderer) {
    // Determine effective heightMode:
    // - Auto-height slots (header/footer): always 'wrap' (content determines height)
    // - Other slots (left/center/right): respect child's heightMode if set, otherwise 'fill'
    const childHeightMode = child.props?.heightMode as string | undefined;
    const effectiveHeightMode = isAutoHeightSlot
      ? 'wrap'
      : (childHeightMode || 'fill');

    // Determine height based on effective heightMode
    const effectiveHeight = effectiveHeightMode === 'wrap' ? 'auto' : '100%';

    const modifiedChild: ComponentInstance = {
      ...child,
      props: {
        ...child.props,
        maxWidth: 'none',
        centerContent: false,
        // Use the effective heightMode (respects user's setting for non-auto-height slots)
        heightMode: effectiveHeightMode,
      },
      size: {
        ...child.size,
        width: '100%' as unknown as number,
        height: effectiveHeight as unknown as number,
      },
    };

    return (
      <div
        style={{
          width: '100%',
          // Match wrapper height to effectiveHeightMode
          height: effectiveHeight,
        }}
      >
        <ChildRenderer
          component={modifiedChild}
          isEditMode={isEditMode}
        />
      </div>
    );
  }

  // Renderer not found - show error (plugins should be preloaded by MultiPagePreview)
  const pluginName = child.pluginId || 'unknown';
  const componentName = child.componentId || 'Component';

  // Use same heightMode logic as above for error fallback
  const childHeightMode = child.props?.heightMode as string | undefined;
  const effectiveHeightMode = isAutoHeightSlot
    ? 'wrap'
    : (childHeightMode || 'fill');
  const effectiveHeight = effectiveHeightMode === 'wrap' ? 'auto' : '100%';

  return (
    <div
      className="slot-child-error"
      style={{
        width: '100%',
        height: effectiveHeight,
        minHeight: effectiveHeightMode === 'wrap' ? '120px' : (childSizeStyles.height || '100px'),
        position: 'relative',
        backgroundColor: '#fee2e2',
        border: '3px solid #dc2626',
        borderRadius: '8px',
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        justifyContent: 'center',
        padding: '20px',
        boxSizing: 'border-box',
      }}
    >
      <div style={{
        width: '48px',
        height: '48px',
        borderRadius: '50%',
        backgroundColor: '#dc2626',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        marginBottom: '12px',
      }}>
        <span style={{ color: 'white', fontSize: '28px', fontWeight: 'bold' }}>!</span>
      </div>
      <div style={{
        color: '#dc2626',
        fontSize: '16px',
        fontWeight: 'bold',
        marginBottom: '8px',
        textAlign: 'center',
      }}>
        Plugin Not Loaded
      </div>
      <div style={{
        backgroundColor: '#fecaca',
        padding: '6px 12px',
        borderRadius: '4px',
        marginBottom: '12px',
      }}>
        <code style={{ color: '#991b1b', fontSize: '14px', fontWeight: '600' }}>
          {componentName}
        </code>
      </div>
      <div style={{
        color: '#7f1d1d',
        fontSize: '13px',
        textAlign: 'center',
        lineHeight: '1.5',
        maxWidth: '300px',
      }}>
        The renderer for <strong>"{componentName}"</strong> from plugin
        <strong> "{pluginName}"</strong> was not found.
      </div>
    </div>
  );
};

interface PageLayoutRendererProps extends RendererProps {
  renderChild?: (child: ComponentInstance) => ReactNode;
}

const PageLayoutRenderer: React.FC<PageLayoutRendererProps> = ({
  component,
  isEditMode,
  renderChild
}) => {
  const props = component.props as PageLayoutProps;
  const styles = component.styles as PageLayoutStyles;

  // State for overlay sidebar toggle (mobile only)
  const [isOverlaySidebarOpen, setIsOverlaySidebarOpen] = useState(false);

  // Fixed layout proportions for the wireframe view
  const headerHeight = '60px';
  const footerHeight = '50px';
  const leftWidth = '180px';

  const {
    gap = '4px',
    fullHeight = true,
    stickyHeader = false,
    stickyFooter = false,
    sidebarRatio = '30-70',
    mobileSidebarBehavior = 'hidden' as MobileSidebarBehavior,
  } = props;

  const {
    backgroundColor = '#f8f9fa',
  } = styles;

  // Group children by their assigned slot
  const children = component.children || [];
  const slottedChildren = useMemo(() => groupChildrenBySlot(children), [children]);

  // Determine which regions have content
  const hasHeader = slottedChildren.header.length > 0;
  const hasFooter = slottedChildren.footer.length > 0;
  const hasLeft = slottedChildren.left.length > 0;
  const hasRight = slottedChildren.right.length > 0;

  // Helper to determine slot overflow based on children's heightMode
  // If any child has heightMode: 'wrap', the slot should use overflow: visible
  // to allow the child to expand and push content down (no scrollbar on slot)
  const getSlotOverflow = (slot: PageLayoutSlot): 'auto' | 'visible' => {
    const children = slottedChildren[slot];
    if (children.length === 0) return 'auto';

    // Check if any child has heightMode: 'wrap'
    const hasWrapChild = children.some(child => child.props?.heightMode === 'wrap');
    return hasWrapChild ? 'visible' : 'auto';
  };

  // Helper to get child component's size styles
  const getChildSizeStyles = (child: ComponentInstance): React.CSSProperties => {
    const sizeStyles: React.CSSProperties = {};

    // Apply width from size if it's a valid pixel/percentage value
    if (child.size?.width) {
      const width = child.size.width;
      if (typeof width === 'string') {
        sizeStyles.width = width;
      } else if (typeof width === 'number' && width > 0) {
        sizeStyles.width = `${width}px`;
      }
    }

    // Apply height from size if it's a valid pixel/percentage value (not 'auto')
    if (child.size?.height) {
      const height = child.size.height;
      if (typeof height === 'string' && height !== 'auto') {
        sizeStyles.height = height;
      } else if (typeof height === 'number' && height > 0) {
        sizeStyles.height = `${height}px`;
      }
    }

    return sizeStyles;
  };

  // Render children for a specific slot
  // Children in PageLayout slots should fill their slot width (no maxWidth constraint)
  // Header and footer slots: children use natural height (auto) - slot auto-sizes to fit
  // Left, right, center slots: children fill 100% height (they're in flex/grid with defined height)
  const renderSlotChildren = (slot: PageLayoutSlot) => {
    const slotChildren = slottedChildren[slot];

    if (slotChildren.length === 0) return null;

    // If renderChild prop is provided, use it
    if (renderChild) {
      return slotChildren.map(child => renderChild(child));
    }

    // Header/footer have auto height, so children should NOT use 100% (100% of auto = 0)
    // Left/right/center are in a flex container with defined height, so 100% works
    const isAutoHeightSlot = slot === 'header' || slot === 'footer';

    // Use SlotChildRenderer which handles async plugin loading
    return slotChildren.map(child => {
      const childSizeStyles = getChildSizeStyles(child);

      return (
        <SlotChildRenderer
          key={child.instanceId}
          child={child}
          isEditMode={isEditMode}
          isAutoHeightSlot={isAutoHeightSlot}
          childSizeStyles={childSizeStyles}
        />
      );
    });
  };

  // Edit mode - Visual wireframe layout
  if (isEditMode) {
    const regionStyle: React.CSSProperties = {
      border: '1px solid #dee2e6',
      backgroundColor: '#ffffff',
      display: 'flex',
      flexDirection: 'column',
      alignItems: 'center',
      justifyContent: 'center',
      overflow: 'hidden',
      position: 'relative',
    };

    const labelStyle: React.CSSProperties = {
      color: '#6c757d',
      fontSize: '14px',
      fontWeight: 500,
      textTransform: 'uppercase',
      letterSpacing: '0.5px',
      pointerEvents: 'none',
    };

    const hasContentStyle: React.CSSProperties = {
      position: 'absolute',
      top: '4px',
      right: '4px',
      width: '8px',
      height: '8px',
      borderRadius: '50%',
      backgroundColor: '#28a745',
    };

    // Mobile behavior badge style for left panel
    const mobileBehaviorBadgeStyle: React.CSSProperties = {
      position: 'absolute',
      bottom: '4px',
      left: '4px',
      padding: '2px 6px',
      borderRadius: '4px',
      fontSize: '10px',
      fontWeight: 600,
      textTransform: 'uppercase',
      zIndex: 10,
      pointerEvents: 'none',
    };

    // Get badge color and text based on mobile behavior
    const getMobileBehaviorBadge = () => {
      switch (mobileSidebarBehavior) {
        case 'overlay':
          return { color: '#fff', bg: '#6366f1', text: 'Overlay on Mobile' };
        case 'stacked':
          return { color: '#fff', bg: '#059669', text: 'Stacked on Mobile' };
        case 'hidden':
        default:
          return { color: '#fff', bg: '#6b7280', text: 'Hidden on Mobile' };
      }
    };

    const mobileBadge = getMobileBehaviorBadge();

    // Component badge style for showing what's in a slot
    const componentBadgeStyle: React.CSSProperties = {
      display: 'inline-flex',
      alignItems: 'center',
      gap: '4px',
      padding: '4px 8px',
      borderRadius: '4px',
      fontSize: '11px',
      fontWeight: 600,
      backgroundColor: '#e0e7ff',
      color: '#4338ca',
      marginTop: '4px',
    };

    // Get component names for a slot
    const getSlotComponentNames = (slot: PageLayoutSlot): string[] => {
      return slottedChildren[slot].map(child => child.componentId || 'Component');
    };

    return (
      <div
        className="page-layout-container edit-mode"
        style={{
          display: 'grid',
          gridTemplateRows: `${headerHeight} 1fr ${footerHeight}`,
          gridTemplateColumns: `${leftWidth} 1fr`,
          gap,
          backgroundColor,
          width: '100%',
          height: fullHeight ? '500px' : '400px',
          border: '2px solid #dee2e6',
          borderRadius: '8px',
          overflow: 'hidden',
        }}
        data-component-type="PageLayout"
      >
        {/* Header - spans full width */}
        <div
          className="page-layout-region page-layout-header"
          style={{
            ...regionStyle,
            gridColumn: '1 / -1',
            gridRow: '1 / 2',
          }}
          data-slot="header"
          data-droppable="true"
        >
          {hasHeader && <div style={hasContentStyle} title="Has content" />}
          <span style={labelStyle}>Header</span>
          {hasHeader && (
            <div style={{ width: '100%', height: '100%', position: 'absolute' }}>
              {renderSlotChildren('header')}
            </div>
          )}
        </div>

        {/* Left Side Panel */}
        <div
          className="page-layout-region page-layout-left"
          style={{
            ...regionStyle,
            gridColumn: '1 / 2',
            gridRow: '2 / 3',
            // Visual distinction for overlay mode
            ...(mobileSidebarBehavior === 'overlay' ? {
              border: '2px dashed #6366f1',
              backgroundColor: '#f5f3ff',
            } : {}),
            // Visual distinction for hidden mode
            ...(mobileSidebarBehavior === 'hidden' ? {
              border: '1px dashed #9ca3af',
              backgroundColor: '#f9fafb',
            } : {}),
          }}
          data-slot="left"
          data-droppable="true"
        >
          {/* Labels and badges container - shown above child content */}
          <div style={{
            position: 'relative',
            zIndex: 10,
            display: 'flex',
            flexDirection: 'column',
            alignItems: 'center',
            padding: '8px',
            backgroundColor: 'rgba(255, 255, 255, 0.9)',
            borderRadius: '4px',
          }}>
            {hasLeft && <div style={{ ...hasContentStyle, position: 'static', marginBottom: '4px' }} title="Has content" />}
            <span style={labelStyle}>Left</span>
            <span style={{ ...labelStyle, fontSize: '12px', marginTop: '2px' }}>Side Panel</span>
            {/* Show component names */}
            {hasLeft && (
              <div style={{ display: 'flex', flexWrap: 'wrap', gap: '4px', marginTop: '8px', justifyContent: 'center' }}>
                {getSlotComponentNames('left').map((name, idx) => (
                  <span key={idx} style={componentBadgeStyle}>{name}</span>
                ))}
              </div>
            )}
            {/* Mobile behavior indicator */}
            <div
              style={{
                ...componentBadgeStyle,
                marginTop: '8px',
                color: mobileBadge.color,
                backgroundColor: mobileBadge.bg,
                fontSize: '9px',
              }}
              title={`This panel will be ${mobileSidebarBehavior} on mobile devices`}
            >
              {mobileBadge.text}
            </div>
          </div>
        </div>

        {/* Content Panel - main area */}
        <div
          className="page-layout-region page-layout-center"
          style={{
            ...regionStyle,
            gridColumn: '2 / 3',
            gridRow: '2 / 3',
          }}
          data-slot="center"
          data-droppable="true"
        >
          {slottedChildren.center.length > 0 && <div style={hasContentStyle} title="Has content" />}
          <span style={labelStyle}>Content Panel</span>
          {slottedChildren.center.length > 0 && (
            <div style={{ width: '100%', height: '100%', position: 'absolute' }}>
              {renderSlotChildren('center')}
            </div>
          )}
        </div>

        {/* Footer - spans full width */}
        <div
          className="page-layout-region page-layout-footer"
          style={{
            ...regionStyle,
            gridColumn: '1 / -1',
            gridRow: '3 / 4',
          }}
          data-slot="footer"
          data-droppable="true"
        >
          {hasFooter && <div style={hasContentStyle} title="Has content" />}
          <span style={labelStyle}>Footer</span>
          {hasFooter && (
            <div style={{ width: '100%', height: '100%', position: 'absolute' }}>
              {renderSlotChildren('footer')}
            </div>
          )}
        </div>
      </div>
    );
  }

  // Preview mode - Actual responsive layout with intelligent region expansion
  // Get responsive configuration
  const responsiveConfig: ResponsiveConfig = props.responsive || DEFAULT_RESPONSIVE_CONFIG;

  // Parse sidebar ratio (e.g., '30-70' -> 30% left, 70% center)
  const [leftPercent, centerPercent] = sidebarRatio.split('-').map((s: string) => parseInt(s, 10));

  // Generate unique ID for this layout instance (for scoped CSS)
  const layoutId = `page-layout-${component.instanceId}`;

  // Generate responsive CSS media queries
  // Only generate rules for slots that actually have content
  const generateResponsiveCSS = (): string => {
    const cssRules: string[] = [];
    const breakpointOrder: BreakpointName[] = ['mobile', 'tablet', 'desktop', 'large'];

    breakpointOrder.forEach((bp) => {
      const bpDef = BREAKPOINTS[bp];
      const settings: BreakpointSettings = responsiveConfig[bp];

      // Build media query
      let mediaQuery: string;
      if (bp === 'large') {
        mediaQuery = `@media (min-width: ${bpDef.minWidth}px)`;
      } else {
        mediaQuery = `@media (min-width: ${bpDef.minWidth}px) and (max-width: ${bpDef.maxWidth}px)`;
      }

      // Generate CSS for this breakpoint
      const rules: string[] = [];

      // Determine which sidebars are VISIBLE at this breakpoint (based on responsive config)
      const hasVisibleLeft = hasLeft && settings.slotVisibility.left;
      const hasVisibleRight = hasRight && settings.slotVisibility.right;

      // For mobile breakpoint, handle mobileSidebarBehavior
      const isMobile = bp === 'mobile';
      const sidebarHiddenByConfig = hasLeft && !settings.slotVisibility.left;

      // Slot visibility - handle based on mobileSidebarBehavior for mobile
      if (hasHeader && !settings.slotVisibility.header) {
        rules.push(`#${layoutId} .page-layout-header { display: none !important; }`);
      }
      if (hasFooter && !settings.slotVisibility.footer) {
        rules.push(`#${layoutId} .page-layout-footer { display: none !important; }`);
      }

      // Handle left sidebar visibility based on mobileSidebarBehavior
      if (hasLeft && !settings.slotVisibility.left) {
        if (isMobile && (mobileSidebarBehavior === 'stacked' || mobileSidebarBehavior === 'overlay')) {
          // Don't hide - will be handled by stacked/overlay logic below
        } else {
          rules.push(`#${layoutId} .page-layout-left { display: none !important; }`);
        }
      }

      if (hasRight && !settings.slotVisibility.right) {
        rules.push(`#${layoutId} .page-layout-right { display: none !important; }`);
      }
      if (slottedChildren.center.length > 0 && !settings.slotVisibility.center) {
        rules.push(`#${layoutId} .page-layout-center { display: none !important; }`);
      }

      // Mobile-specific sidebar behavior
      if (isMobile && hasLeft && sidebarHiddenByConfig) {
        if (mobileSidebarBehavior === 'stacked') {
          // Stacked: Show sidebar above center content
          rules.push(`#${layoutId} .page-layout-middle { display: flex !important; flex-direction: column !important; }`);
          rules.push(`#${layoutId} .page-layout-left { display: flex !important; width: 100% !important; order: -1 !important; }`);
          rules.push(`#${layoutId} .page-layout-center { width: 100% !important; flex: 1 !important; }`);
        } else if (mobileSidebarBehavior === 'overlay') {
          // Overlay: Sidebar is positioned as overlay (controlled by JS state)
          // Use absolute positioning relative to .page-layout-middle (which has position: relative)
          // This keeps the overlay within the middle content area (between header and footer)
          // Hide by default, show when .sidebar-open class is added
          rules.push(`#${layoutId} .page-layout-middle { position: relative !important; }`);
          rules.push(`#${layoutId} .page-layout-left {
            display: flex !important;
            position: absolute !important;
            top: 0 !important;
            left: 0 !important;
            width: 280px !important;
            max-width: 85% !important;
            height: 100% !important;
            z-index: 1000 !important;
            transform: translateX(-100%) !important;
            transition: transform 0.3s ease-in-out !important;
            box-shadow: 2px 0 8px rgba(0,0,0,0.15) !important;
            overflow-y: auto !important;
          }`);
          rules.push(`#${layoutId}.sidebar-open .page-layout-left { transform: translateX(0) !important; }`);
          rules.push(`#${layoutId} .sidebar-overlay {
            display: none;
            position: absolute;
            top: 0;
            left: 0;
            right: 0;
            bottom: 0;
            background: rgba(0,0,0,0.5);
            z-index: 999;
            opacity: 0;
            transition: opacity 0.3s ease-in-out;
          }`);
          rules.push(`#${layoutId}.sidebar-open .sidebar-overlay { display: block; opacity: 1; }`);
          // Center takes full width
          rules.push(`#${layoutId} .page-layout-middle { display: flex !important; flex-direction: column !important; }`);
          rules.push(`#${layoutId} .page-layout-center { width: 100% !important; flex: 1 !important; }`);
          // Show hamburger menu button
          rules.push(`#${layoutId} .hamburger-menu { display: flex !important; }`);
        } else {
          // Hidden (default): Completely hide sidebar
          rules.push(`#${layoutId} .page-layout-left { display: none !important; }`);
          rules.push(`#${layoutId} .page-layout-middle { display: flex !important; flex-direction: column !important; }`);
          rules.push(`#${layoutId} .page-layout-center { width: 100% !important; flex: 1 !important; }`);
        }
        // Hide hamburger on non-overlay mode
        if (mobileSidebarBehavior !== 'overlay') {
          rules.push(`#${layoutId} .hamburger-menu { display: none !important; }`);
        }
      } else {
        // Non-mobile or sidebar is visible by config
        // Hide hamburger menu on larger screens
        rules.push(`#${layoutId} .hamburger-menu { display: none !important; }`);

        // Hide overlay backdrop on non-mobile
        rules.push(`#${layoutId} .sidebar-overlay { display: none !important; }`);

        // Reset sidebar to normal grid positioning (undo overlay styles from mobile)
        if (hasLeft && settings.slotVisibility.left) {
          rules.push(`#${layoutId} .page-layout-left {
            position: static !important;
            transform: none !important;
            width: 100% !important;
            max-width: none !important;
            height: auto !important;
            z-index: auto !important;
            box-shadow: none !important;
          }`);
        }

        // Stacking behavior - only apply if there are sidebars that are VISIBLE at this breakpoint
        if (settings.stackSidebars && (hasVisibleLeft || hasVisibleRight)) {
          rules.push(`#${layoutId} .page-layout-middle { display: flex !important; flex-direction: column !important; }`);
          rules.push(`#${layoutId} .page-layout-middle > * { width: 100% !important; }`);
        } else if (hasVisibleLeft || hasVisibleRight) {
          // Reset to grid layout for non-stacking mode
          rules.push(`#${layoutId} .page-layout-middle { display: grid !important; }`);
        }

        // When sidebars exist but are HIDDEN at this breakpoint, fix the grid layout
        if ((hasLeft && !settings.slotVisibility.left) || (hasRight && !settings.slotVisibility.right)) {
          if (!hasVisibleLeft && !hasVisibleRight) {
            rules.push(`#${layoutId} .page-layout-middle { display: flex !important; flex-direction: column !important; }`);
            rules.push(`#${layoutId} .page-layout-center { width: 100% !important; flex: 1 !important; }`);
          } else if (hasVisibleLeft && !hasVisibleRight) {
            rules.push(`#${layoutId} .page-layout-middle { grid-template-columns: ${leftPercent}% 1fr !important; }`);
          } else if (!hasVisibleLeft && hasVisibleRight) {
            rules.push(`#${layoutId} .page-layout-middle { grid-template-columns: 1fr 250px !important; }`);
          }
        }
      }

      if (rules.length > 0) {
        cssRules.push(`${mediaQuery} {\n  ${rules.join('\n  ')}\n}`);
      }
    });

    return cssRules.join('\n\n');
  };

  // Build grid template based on which regions have content
  const gridTemplateRows = [
    hasHeader ? 'auto' : '',
    '1fr',
    hasFooter ? 'auto' : '',
  ].filter(Boolean).join(' ');

  // Grid columns calculation - center should always fill remaining space
  const gridTemplateColumns = (() => {
    if (!hasLeft && !hasRight) {
      return '1fr'; // No sidebars, center takes full width
    }
    if (hasLeft && !hasRight) {
      return `${leftPercent}% 1fr`; // Left sidebar + center takes rest
    }
    if (!hasLeft && hasRight) {
      return `1fr 250px`; // Center takes rest + right sidebar
    }
    // Both sidebars present
    return `${leftPercent}% 1fr 250px`;
  })();

  // Calculate grid positions based on what regions exist
  const getPreviewRegionStyle = (slot: PageLayoutSlot): React.CSSProperties => {
    const baseStyle: React.CSSProperties = {
      display: 'flex',
      flexDirection: 'column',
      // Overflow depends on child's heightMode:
      // - wrap: visible (slot expands with child, no scrollbar)
      // - fill/resizable: auto (scrollbar when content overflows)
      overflow: getSlotOverflow(slot),
    };

    const rowStart = hasHeader ? 2 : 1;
    const rowEnd = rowStart + 1;
    const colStart = hasLeft ? 2 : 1;
    const colEnd = colStart + 1;
    const totalCols = (hasLeft ? 1 : 0) + 1 + (hasRight ? 1 : 0);

    switch (slot) {
      case 'header':
        return {
          ...baseStyle,
          gridRow: '1 / 2',
          gridColumn: `1 / ${totalCols + 1}`,
          minHeight: '60px',
          position: stickyHeader ? 'sticky' : undefined,
          top: stickyHeader ? 0 : undefined,
          zIndex: stickyHeader ? 100 : undefined,
        };
      case 'footer':
        return {
          ...baseStyle,
          gridRow: `${hasHeader ? 3 : 2} / ${hasHeader ? 4 : 3}`,
          gridColumn: `1 / ${totalCols + 1}`,
          minHeight: '50px',
          position: stickyFooter ? 'sticky' : undefined,
          bottom: stickyFooter ? 0 : undefined,
          zIndex: stickyFooter ? 100 : undefined,
        };
      case 'left':
        return {
          ...baseStyle,
          gridRow: `${rowStart} / ${rowEnd}`,
          gridColumn: '1 / 2',
        };
      case 'right':
        return {
          ...baseStyle,
          gridRow: `${rowStart} / ${rowEnd}`,
          gridColumn: `${colEnd} / ${colEnd + 1}`,
        };
      case 'center':
      default:
        return {
          ...baseStyle,
          gridRow: `${rowStart} / ${rowEnd}`,
          gridColumn: `${colStart} / ${colEnd}`,
          flex: 1,
        };
    }
  };

  // Generate responsive CSS
  const responsiveCSS = generateResponsiveCSS();

  // When sticky header/footer is enabled, use sticky positioning within a scroll container
  if (stickyHeader || stickyFooter) {
    // Grid columns for middle section - use 1fr for full width when no sidebars
    // When left sidebar exists but not right, center should take remaining space (1fr)
    // When both sidebars exist, use percentage-based layout
    const middleGridColumns = (() => {
      if (!hasLeft && !hasRight) {
        return '1fr'; // No sidebars, center takes full width
      }
      if (hasLeft && !hasRight) {
        return `${leftPercent}% 1fr`; // Left sidebar + center takes rest
      }
      if (!hasLeft && hasRight) {
        return `1fr 250px`; // Center takes rest + right sidebar
      }
      // Both sidebars present
      return `${leftPercent}% 1fr 250px`;
    })();

    // Toggle overlay sidebar
    const toggleOverlaySidebar = () => setIsOverlaySidebarOpen(!isOverlaySidebarOpen);
    const closeOverlaySidebar = () => setIsOverlaySidebarOpen(false);

    // Check if center slot has wrap mode children - if so, allow page to expand naturally
    // When center has wrap mode, the page should use the browser's scrollbar, not a nested one
    const centerHasWrapMode = getSlotOverflow('center') === 'visible';

    // When center has wrap mode:
    // - Don't constrain height to 100vh (let page expand naturally)
    // - Use overflow: visible (let browser handle scrolling)
    // When center doesn't have wrap mode:
    // - Use 100vh height for sticky header/footer behavior
    // - Use overflow: auto (enable scrollbar within container)
    const containerHeight = centerHasWrapMode ? 'auto' : (fullHeight ? '100vh' : 'auto');
    const containerOverflow = centerHasWrapMode ? 'visible' : 'auto';
    const containerMinHeight = centerHasWrapMode ? (fullHeight ? '100vh' : undefined) : (fullHeight ? '100vh' : undefined);

    return (
      <>
        {responsiveCSS && (
          <style dangerouslySetInnerHTML={{ __html: responsiveCSS }} />
        )}

        <div
          id={layoutId}
          className={`page-layout-container preview-mode ${isOverlaySidebarOpen ? 'sidebar-open' : ''}`}
          style={{
            display: 'flex',
            flexDirection: 'column',
            backgroundColor,
            width: '100%',
            // When center has wrap mode, allow natural page expansion for browser scrollbar
            // Otherwise use 100vh for sticky header/footer scroll container behavior
            height: containerHeight,
            minHeight: containerMinHeight,
            overflow: containerOverflow,
            position: 'relative',
          }}
          data-component-type="PageLayout"
        >
          {/* Sticky Header - uses position sticky within scrollable parent */}
          {hasHeader && (
            <div
              className="page-layout-region page-layout-header"
              style={{
                position: stickyHeader ? 'sticky' : 'relative',
                top: stickyHeader ? 0 : undefined,
                zIndex: stickyHeader ? 100 : undefined,
                width: '100%',
                flexShrink: 0,
                // Don't set backgroundColor - let the header content's background show through
                // Use flex to allow children to fill height
                display: 'flex',
                flexDirection: 'row',
                alignItems: 'stretch', // Stretch children to fill header height
                minHeight: 0, // Allow content to determine height
              }}
              data-slot="header"
            >
              {/* Hamburger menu button for overlay mode - positioned absolute within header */}
              {mobileSidebarBehavior === 'overlay' && hasLeft && (
                <button
                  className="hamburger-menu"
                  onClick={toggleOverlaySidebar}
                  aria-label={isOverlaySidebarOpen ? 'Close menu' : 'Open menu'}
                  style={{
                    display: 'none', // Hidden by default, shown via CSS on mobile
                    position: 'absolute',
                    top: '50%',
                    left: '8px',
                    transform: 'translateY(-50%)',
                    zIndex: 101,
                    width: '40px',
                    height: '40px',
                    minWidth: '40px',
                    border: 'none',
                    borderRadius: '6px',
                    backgroundColor: 'rgba(0,0,0,0.7)',
                    color: 'white',
                    cursor: 'pointer',
                    alignItems: 'center',
                    justifyContent: 'center',
                    flexDirection: 'column',
                    gap: '4px',
                    padding: '8px',
                  }}
                >
                  {/* Hamburger icon lines */}
                  <span style={{
                    display: 'block',
                    width: '18px',
                    height: '2px',
                    backgroundColor: 'white',
                    borderRadius: '1px',
                    transition: 'transform 0.2s',
                    transform: isOverlaySidebarOpen ? 'rotate(45deg) translate(4px, 4px)' : 'none',
                  }} />
                  <span style={{
                    display: 'block',
                    width: '18px',
                    height: '2px',
                    backgroundColor: 'white',
                    borderRadius: '1px',
                    opacity: isOverlaySidebarOpen ? 0 : 1,
                    transition: 'opacity 0.2s',
                  }} />
                  <span style={{
                    display: 'block',
                    width: '18px',
                    height: '2px',
                    backgroundColor: 'white',
                    borderRadius: '1px',
                    transition: 'transform 0.2s',
                    transform: isOverlaySidebarOpen ? 'rotate(-45deg) translate(4px, -4px)' : 'none',
                  }} />
                </button>
              )}
              {/* Header content wrapper - fills full header area */}
              <div style={{
                flex: 1,
                display: 'flex',
                flexDirection: 'column',
                minHeight: 0,
                // Ensure children fill full height
                alignItems: 'stretch',
              }}>
                {renderSlotChildren('header')}
              </div>
            </div>
          )}

          {/* Hamburger menu button when no header exists - shown as floating button */}
          {mobileSidebarBehavior === 'overlay' && hasLeft && !hasHeader && (
            <button
              className="hamburger-menu"
              onClick={toggleOverlaySidebar}
              aria-label={isOverlaySidebarOpen ? 'Close menu' : 'Open menu'}
              style={{
                display: 'none', // Hidden by default, shown via CSS on mobile
                position: 'absolute',
                top: '12px',
                left: '12px',
                zIndex: 101,
                width: '40px',
                height: '40px',
                border: 'none',
                borderRadius: '6px',
                backgroundColor: 'rgba(0,0,0,0.7)',
                color: 'white',
                cursor: 'pointer',
                alignItems: 'center',
                justifyContent: 'center',
                flexDirection: 'column',
                gap: '4px',
                padding: '8px',
                boxShadow: '0 2px 8px rgba(0,0,0,0.2)',
              }}
            >
              <span style={{
                display: 'block',
                width: '18px',
                height: '2px',
                backgroundColor: 'white',
                borderRadius: '1px',
                transition: 'transform 0.2s',
                transform: isOverlaySidebarOpen ? 'rotate(45deg) translate(4px, 4px)' : 'none',
              }} />
              <span style={{
                display: 'block',
                width: '18px',
                height: '2px',
                backgroundColor: 'white',
                borderRadius: '1px',
                opacity: isOverlaySidebarOpen ? 0 : 1,
                transition: 'opacity 0.2s',
              }} />
              <span style={{
                display: 'block',
                width: '18px',
                height: '2px',
                backgroundColor: 'white',
                borderRadius: '1px',
                transition: 'transform 0.2s',
                transform: isOverlaySidebarOpen ? 'rotate(-45deg) translate(4px, -4px)' : 'none',
              }} />
            </button>
          )}

          {/* Middle content area with sidebar grid */}
          <div
            className="page-layout-middle"
            style={{
              display: hasLeft || hasRight ? 'grid' : 'flex',
              gridTemplateColumns: hasLeft || hasRight ? middleGridColumns : undefined,
              flexDirection: hasLeft || hasRight ? undefined : 'column',
              // When center has wrap mode, use flex: 1 0 auto to allow expansion beyond viewport
              // Otherwise use flex: 1 for normal fill behavior
              flex: centerHasWrapMode ? '1 0 auto' : 1,
              // When center has wrap mode, don't constrain height - allow expansion
              // Otherwise use minHeight: 0 to enable scrolling within fixed container
              minHeight: centerHasWrapMode ? undefined : 0,
              width: '100%',
              position: 'relative', // For overlay sidebar positioning
              // When center has wrap mode, allow content to expand
              overflow: centerHasWrapMode ? 'visible' : undefined,
            }}
          >
            {/* Overlay backdrop for mobile sidebar - inside middle area */}
            {mobileSidebarBehavior === 'overlay' && hasLeft && (
              <div
                className="sidebar-overlay"
                onClick={closeOverlaySidebar}
                style={{ cursor: 'pointer' }}
              />
            )}

            {hasLeft && (
              <div
                className="page-layout-region page-layout-left"
                style={{
                  display: 'flex',
                  flexDirection: 'column',
                  width: '100%', // Fill the grid cell
                  minWidth: 0, // Prevent overflow in grid
                  // Overflow depends on child's heightMode:
                  // - wrap: visible (slot expands with child, no scrollbar)
                  // - fill/resizable: auto (scrollbar when content overflows)
                  overflow: getSlotOverflow('left'),
                }}
                data-slot="left"
              >
                {renderSlotChildren('left')}
              </div>
            )}

            <div
              className="page-layout-region page-layout-center"
              style={{
                display: 'flex',
                flexDirection: 'column',
                width: '100%',
                minWidth: 0, // Prevent overflow in grid
                // When in flex layout (no sidebars) and wrap mode, use flex: 1 0 auto to expand
                // In grid layout, flex has no effect
                flex: centerHasWrapMode && !(hasLeft || hasRight) ? '1 0 auto' : undefined,
                // Overflow depends on child's heightMode
                overflow: getSlotOverflow('center'),
              }}
              data-slot="center"
            >
              {renderSlotChildren('center')}
            </div>

            {hasRight && (
              <div
                className="page-layout-region page-layout-right"
                style={{
                  display: 'flex',
                  flexDirection: 'column',
                  width: '100%', // Fill the grid cell
                  minWidth: 0, // Prevent overflow in grid
                  // Overflow depends on child's heightMode
                  overflow: getSlotOverflow('right'),
                }}
                data-slot="right"
              >
                {renderSlotChildren('right')}
              </div>
            )}
          </div>

          {/* Sticky Footer */}
          {hasFooter && (
            <div
              className="page-layout-region page-layout-footer"
              style={{
                position: stickyFooter ? 'sticky' : 'relative',
                bottom: stickyFooter ? 0 : undefined,
                zIndex: stickyFooter ? 100 : undefined,
                width: '100%',
                flexShrink: 0,
                backgroundColor: backgroundColor, // Ensure footer has background
                // Use flex to allow children to fill height
                display: 'flex',
                flexDirection: 'column',
                // DEBUG: Add min-height to ensure footer is visible
                minHeight: '60px',
              }}
              data-slot="footer"
            >
              {renderSlotChildren('footer')}
            </div>
          )}
        </div>
      </>
    );
  }

  // Standard grid layout (no sticky elements)
  // Check if center slot has wrap mode children - if so, allow page to expand naturally
  const standardCenterHasWrapMode = getSlotOverflow('center') === 'visible';

  return (
    <>
      {/* Inject responsive CSS media queries */}
      {responsiveCSS && (
        <style dangerouslySetInnerHTML={{ __html: responsiveCSS }} />
      )}

      <div
        id={layoutId}
        className="page-layout-container preview-mode"
        style={{
          display: 'grid',
          gridTemplateRows,
          gridTemplateColumns,
          gap: '0',
          backgroundColor,
          // When center has wrap mode, use auto height to allow natural page expansion
          // Otherwise use minHeight for full viewport behavior
          minHeight: standardCenterHasWrapMode ? undefined : (fullHeight ? '100vh' : undefined),
          width: '100%',
          // When center has wrap mode, allow content to expand (browser handles scrolling)
          overflow: standardCenterHasWrapMode ? 'visible' : undefined,
        }}
        data-component-type="PageLayout"
      >
        {hasHeader && (
          <div
            className="page-layout-region page-layout-header"
            style={getPreviewRegionStyle('header')}
            data-slot="header"
          >
            {renderSlotChildren('header')}
          </div>
        )}

        {hasLeft && (
          <div
            className="page-layout-region page-layout-left"
            style={getPreviewRegionStyle('left')}
            data-slot="left"
          >
            {renderSlotChildren('left')}
          </div>
        )}

        <div
          className="page-layout-region page-layout-center"
          style={getPreviewRegionStyle('center')}
          data-slot="center"
        >
          {renderSlotChildren('center')}
        </div>

        {hasRight && (
          <div
            className="page-layout-region page-layout-right"
            style={getPreviewRegionStyle('right')}
            data-slot="right"
          >
            {renderSlotChildren('right')}
          </div>
        )}

        {hasFooter && (
          <div
            className="page-layout-region page-layout-footer"
            style={getPreviewRegionStyle('footer')}
            data-slot="footer"
          >
            {renderSlotChildren('footer')}
          </div>
        )}
      </div>
    </>
  );
};

export default PageLayoutRenderer;
export { PageLayoutRenderer };
