import React from 'react';

/**
 * Container Layout Component
 * A flexible container that holds child components with various layout types
 */
const Container = ({ layoutType, padding, maxWidth, centerContent, allowOverflow, children, styles }) => {
  // Get layout-specific styles based on layoutType
  const getLayoutStyles = () => {
    switch (layoutType) {
      case 'flex-column':
        return {
          display: 'flex',
          flexDirection: 'column',
          gap: '16px',
          alignItems: 'stretch'
        };

      case 'flex-row':
        return {
          display: 'flex',
          flexDirection: 'row',
          gap: '16px',
          alignItems: 'flex-start'
        };

      case 'flex-wrap':
        return {
          display: 'flex',
          flexDirection: 'row',
          flexWrap: 'wrap',
          gap: '16px',
          alignItems: 'flex-start'
        };

      case 'grid-2col':
        return {
          display: 'grid',
          gridTemplateColumns: 'repeat(2, 1fr)',
          gap: '16px'
        };

      case 'grid-3col':
        return {
          display: 'grid',
          gridTemplateColumns: 'repeat(3, 1fr)',
          gap: '16px'
        };

      case 'grid-4col':
        return {
          display: 'grid',
          gridTemplateColumns: 'repeat(4, 1fr)',
          gap: '16px'
        };

      case 'grid-auto':
        return {
          display: 'grid',
          gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))',
          gap: '16px'
        };

      default:
        return {
          display: 'flex',
          flexDirection: 'column',
          gap: '16px'
        };
    }
  };

  const containerStyles = {
    padding: padding || '20px',
    maxWidth: maxWidth && maxWidth !== 'none' ? maxWidth : undefined,
    margin: centerContent ? '0 auto' : undefined,
    overflow: allowOverflow ? 'visible' : 'auto',
    backgroundColor: '#ffffff',
    borderRadius: '8px',
    boxShadow: '0 1px 3px rgba(0,0,0,0.1)',
    minHeight: 'auto',
    height: 'auto',
    width: '100%',
    boxSizing: 'border-box',
    ...getLayoutStyles(),
    ...styles // User custom styles override defaults
  };

  return (
    <div style={containerStyles} className="container-layout">
      {children && children.length > 0 ? (
        children
      ) : (
        <div style={{
          padding: '40px',
          textAlign: 'center',
          color: '#999',
          border: '2px dashed #ddd',
          borderRadius: '8px',
          backgroundColor: '#f9f9f9',
          gridColumn: '1 / -1' // Span all columns in grid layout
        }}>
          Drop components here
        </div>
      )}
    </div>
  );
};

export default Container;
