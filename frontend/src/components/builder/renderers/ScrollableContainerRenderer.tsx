import React, { useRef, useEffect, useState } from 'react';
import { RendererProps } from './RendererRegistry';

/**
 * ScrollableContainerRenderer - Renders a scrollable container component
 * Supports vertical, horizontal, or both scroll directions with customizable scrollbar styling
 *
 * File naming convention: {ComponentName}Renderer.tsx
 * The component name "ScrollableContainer" is derived from filename "ScrollableContainerRenderer.tsx"
 */
const ScrollableContainerRenderer: React.FC<RendererProps> = ({ component, isEditMode }) => {
  const containerRef = useRef<HTMLDivElement>(null);
  const [uniqueId] = useState(() => `scroll-${component.instanceId.slice(0, 8)}`);

  // Extract props with defaults
  const {
    scrollDirection = 'vertical',
    smoothScroll = true,
    hideScrollbar = false,
    scrollSnapType = 'none',
    scrollSnapAlign = 'start',
    height = '400px',
    width = '100%',
    maxHeight = 'none',
    maxWidth = 'none',
    layoutType = 'flex-column',
    padding = '16px',
    gap = '16px',
    scrollbarWidth = 'thin',
    scrollbarColor = '#888888',
    scrollbarTrackColor = '#f1f1f1',
  } = component.props;

  // Extract styles
  const {
    backgroundColor = '#ffffff',
    borderRadius = '8px',
    border = '1px solid #e0e0e0',
    boxShadow = '0 2px 4px rgba(0,0,0,0.05)',
  } = component.styles as Record<string, string>;

  // Get overflow styles based on scroll direction
  const getOverflowStyles = (): React.CSSProperties => {
    switch (scrollDirection) {
      case 'horizontal':
        return {
          overflowX: 'auto',
          overflowY: 'hidden',
        };
      case 'both':
        return {
          overflowX: 'auto',
          overflowY: 'auto',
        };
      case 'vertical':
      default:
        return {
          overflowX: 'hidden',
          overflowY: 'auto',
        };
    }
  };

  // Get layout styles based on layoutType
  const getLayoutStyles = (): React.CSSProperties => {
    switch (layoutType) {
      case 'flex-row':
        return {
          display: 'flex',
          flexDirection: 'row',
          flexWrap: 'nowrap',
        };
      case 'flex-wrap':
        return {
          display: 'flex',
          flexDirection: 'row',
          flexWrap: 'wrap',
        };
      case 'grid-2col':
        return {
          display: 'grid',
          gridTemplateColumns: 'repeat(2, 1fr)',
        };
      case 'grid-3col':
        return {
          display: 'grid',
          gridTemplateColumns: 'repeat(3, 1fr)',
        };
      case 'grid-auto':
        return {
          display: 'grid',
          gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))',
        };
      case 'flex-column':
      default:
        return {
          display: 'flex',
          flexDirection: 'column',
        };
    }
  };

  // Get scroll snap styles
  const getScrollSnapStyles = (): React.CSSProperties => {
    if (scrollSnapType === 'none') {
      return {};
    }

    const snapAxis = scrollDirection === 'horizontal' ? 'x' : scrollDirection === 'both' ? 'both' : 'y';

    return {
      scrollSnapType: `${snapAxis} ${scrollSnapType}`,
    };
  };

  // Generate dynamic CSS for scrollbar styling
  const scrollbarStyles = `
    .${uniqueId} {
      scrollbar-width: ${scrollbarWidth};
      scrollbar-color: ${scrollbarColor} ${scrollbarTrackColor};
    }

    .${uniqueId}::-webkit-scrollbar {
      width: ${scrollbarWidth === 'thin' ? '8px' : scrollbarWidth === 'none' ? '0px' : '12px'};
      height: ${scrollbarWidth === 'thin' ? '8px' : scrollbarWidth === 'none' ? '0px' : '12px'};
    }

    .${uniqueId}::-webkit-scrollbar-track {
      background: ${scrollbarTrackColor};
      border-radius: 4px;
    }

    .${uniqueId}::-webkit-scrollbar-thumb {
      background: ${scrollbarColor};
      border-radius: 4px;
      border: 2px solid ${scrollbarTrackColor};
    }

    .${uniqueId}::-webkit-scrollbar-thumb:hover {
      background: ${adjustColor(scrollbarColor as string, -20)};
    }

    ${hideScrollbar ? `
    .${uniqueId} {
      scrollbar-width: none;
      -ms-overflow-style: none;
    }
    .${uniqueId}::-webkit-scrollbar {
      display: none;
    }
    ` : ''}

    /* Scroll snap for children */
    ${scrollSnapType !== 'none' ? `
    .${uniqueId} > * {
      scroll-snap-align: ${scrollSnapAlign};
    }
    ` : ''}
  `;

  // Helper function to adjust color brightness
  function adjustColor(color: string, amount: number): string {
    // Simple color adjustment - works with hex colors
    if (color.startsWith('#')) {
      let hex = color.slice(1);
      if (hex.length === 3) {
        hex = hex.split('').map(c => c + c).join('');
      }
      const num = parseInt(hex, 16);
      const r = Math.min(255, Math.max(0, (num >> 16) + amount));
      const g = Math.min(255, Math.max(0, ((num >> 8) & 0x00FF) + amount));
      const b = Math.min(255, Math.max(0, (num & 0x0000FF) + amount));
      return `#${((r << 16) | (g << 8) | b).toString(16).padStart(6, '0')}`;
    }
    return color;
  }

  // Base container styles
  const containerStyles: React.CSSProperties = {
    ...getOverflowStyles(),
    ...getLayoutStyles(),
    ...getScrollSnapStyles(),
    scrollBehavior: smoothScroll ? 'smooth' : 'auto',
    height: height !== 'auto' ? height : undefined,
    width: width,
    maxHeight: maxHeight !== 'none' ? maxHeight : undefined,
    maxWidth: maxWidth !== 'none' ? maxWidth : undefined,
    padding: padding,
    gap: gap,
    backgroundColor,
    borderRadius,
    border,
    boxShadow,
    boxSizing: 'border-box',
    position: 'relative',
    // Apply additional styles from component
    ...(component.styles as React.CSSProperties),
  };

  // Edit mode indicator styles
  const editModeIndicatorStyles: React.CSSProperties = {
    position: 'absolute',
    top: '4px',
    right: '4px',
    fontSize: '10px',
    padding: '2px 6px',
    background: 'rgba(0, 123, 255, 0.1)',
    color: '#007bff',
    borderRadius: '3px',
    pointerEvents: 'none',
    zIndex: 10,
  };

  // Scroll direction indicator for edit mode
  const getScrollIndicator = () => {
    switch (scrollDirection) {
      case 'horizontal':
        return '↔ Horizontal Scroll';
      case 'both':
        return '↔↕ Both Directions';
      case 'vertical':
      default:
        return '↕ Vertical Scroll';
    }
  };

  return (
    <>
      {/* Inject dynamic scrollbar styles */}
      <style>{scrollbarStyles}</style>

      <div
        ref={containerRef}
        style={containerStyles}
        className={`scrollable-container-renderer ${uniqueId} ${isEditMode ? 'edit-mode' : 'preview-mode'}`}
      >
        {/* Edit mode indicator */}
        {isEditMode && (
          <div style={editModeIndicatorStyles}>
            {getScrollIndicator()}
          </div>
        )}

        {/* Empty state for edit mode */}
        {isEditMode && (!component.children || component.children.length === 0) && (
          <div
            style={{
              display: 'flex',
              flexDirection: 'column',
              alignItems: 'center',
              justifyContent: 'center',
              minHeight: '200px',
              color: '#999',
              textAlign: 'center',
              padding: '20px',
            }}
          >
            <div style={{ fontSize: '32px', marginBottom: '8px', opacity: 0.5 }}>
              {scrollDirection === 'horizontal' ? '↔' : scrollDirection === 'both' ? '↔↕' : '↕'}
            </div>
            <div style={{ fontSize: '14px', fontWeight: 500 }}>
              Scrollable Container
            </div>
            <div style={{ fontSize: '12px', marginTop: '4px' }}>
              Drop components here to make them scrollable
            </div>
          </div>
        )}

        {/* Children are rendered by BuilderCanvas in edit mode */}
        {/* In preview mode, children would be passed through the component tree */}
      </div>
    </>
  );
};

export default ScrollableContainerRenderer;
export { ScrollableContainerRenderer };
