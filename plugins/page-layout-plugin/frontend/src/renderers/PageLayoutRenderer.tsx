import React, { useMemo, ReactNode } from 'react';
import type { RendererProps, PageLayoutProps, PageLayoutStyles, PageLayoutSlot, ComponentInstance, ResponsiveConfig, BreakpointSettings } from '../types';
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
    const modifiedChild: ComponentInstance = {
      ...child,
      props: {
        ...child.props,
        maxWidth: 'none',
        centerContent: false,
        heightMode: isAutoHeightSlot ? 'wrap' : 'fill',
      },
      size: {
        ...child.size,
        width: '100%' as unknown as number,
        height: isAutoHeightSlot ? ('auto' as unknown as number) : ('100%' as unknown as number),
      },
    };

    return (
      <div
        style={{
          width: '100%',
          height: isAutoHeightSlot ? 'auto' : '100%',
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

  return (
    <div
      className="slot-child-error"
      style={{
        width: '100%',
        height: isAutoHeightSlot ? 'auto' : '100%',
        minHeight: isAutoHeightSlot ? '120px' : (childSizeStyles.height || '100px'),
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
          }}
          data-slot="left"
          data-droppable="true"
        >
          {hasLeft && <div style={hasContentStyle} title="Has content" />}
          <span style={labelStyle}>Left</span>
          <span style={{ ...labelStyle, fontSize: '12px', marginTop: '2px' }}>Side Panel</span>
          {hasLeft && (
            <div style={{ width: '100%', height: '100%', position: 'absolute' }}>
              {renderSlotChildren('left')}
            </div>
          )}
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

      // Slot visibility - only hide slots that have content but should be hidden at this breakpoint
      if (hasHeader && !settings.slotVisibility.header) {
        rules.push(`#${layoutId} .page-layout-header { display: none !important; }`);
      }
      if (hasFooter && !settings.slotVisibility.footer) {
        rules.push(`#${layoutId} .page-layout-footer { display: none !important; }`);
      }
      if (hasLeft && !settings.slotVisibility.left) {
        rules.push(`#${layoutId} .page-layout-left { display: none !important; }`);
      }
      if (hasRight && !settings.slotVisibility.right) {
        rules.push(`#${layoutId} .page-layout-right { display: none !important; }`);
      }
      if (slottedChildren.center.length > 0 && !settings.slotVisibility.center) {
        rules.push(`#${layoutId} .page-layout-center { display: none !important; }`);
      }

      // Stacking behavior - only apply if there are sidebars with content
      if (settings.stackSidebars && (hasLeft || hasRight)) {
        rules.push(`#${layoutId} .page-layout-middle { display: flex !important; flex-direction: column !important; }`);
        rules.push(`#${layoutId} .page-layout-middle > * { width: 100% !important; }`);
      }
      // Note: We don't generate width rules for non-stacking layouts anymore.
      // The grid-template-columns on .page-layout-middle already handles the sidebar widths correctly.
      // Setting explicit width on grid children would override the grid cell sizing.

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
      overflow: 'auto',
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

    return (
      <>
        {responsiveCSS && (
          <style dangerouslySetInnerHTML={{ __html: responsiveCSS }} />
        )}

        <div
          id={layoutId}
          className="page-layout-container preview-mode"
          style={{
            display: 'flex',
            flexDirection: 'column',
            backgroundColor,
            width: '100%',
            // Use 100vh for full-height sticky behavior - this ensures scroll container has defined height
            height: fullHeight ? '100vh' : 'auto',
            minHeight: fullHeight ? '100vh' : undefined,
            overflow: 'auto', // Make this container scrollable for sticky to work
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
                backgroundColor: backgroundColor, // Ensure header has background to cover content
                // Use flex to allow children to fill height
                display: 'flex',
                flexDirection: 'column',
              }}
              data-slot="header"
            >
              {renderSlotChildren('header')}
            </div>
          )}

          {/* Middle content area with sidebar grid */}
          <div
            className="page-layout-middle"
            style={{
              display: hasLeft || hasRight ? 'grid' : 'flex',
              gridTemplateColumns: hasLeft || hasRight ? middleGridColumns : undefined,
              flexDirection: hasLeft || hasRight ? undefined : 'column',
              flex: 1,
              minHeight: 0,
              width: '100%',
            }}
          >
            {hasLeft && (
              <div
                className="page-layout-region page-layout-left"
                style={{
                  display: 'flex',
                  flexDirection: 'column',
                  width: '100%', // Fill the grid cell
                  minWidth: 0, // Prevent overflow in grid
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
          minHeight: fullHeight ? '100vh' : undefined,
          width: '100%',
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
