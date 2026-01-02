import React, { useState } from 'react';
import type { RendererProps } from '../types';

/**
 * HorizontalRowRenderer - Renders a horizontal row component with configurable properties
 */
const HorizontalRowRenderer: React.FC<RendererProps> = ({ component, isEditMode }) => {
  const [isLoaded, setIsLoaded] = useState(false);
  const [hasError, setHasError] = useState(false);

  const props = component.props || {};
  const lazyLoad = props.lazyLoad !== false;

  // Determine if we're inside a parent container or at root level
  const hasParent = !!component.parentId;

  return (
    <div style={containerStyles}>
      <div style={imageWrapperStyles}>
        {!hasError ? (
          <>
            <div style={placeholderStyles}>
              {isEditMode && !src ? 'No image selected' : 'Loading...'}
            </div>
            <img
              src={src}
              alt={alt}
              style={imageStyles}
              onLoad={handleImageLoad}
              onError={handleImageError}
              loading={lazyLoad ? 'lazy' : 'eager'}
            />
          </>
        ) : (
          <div style={errorStyles}>
            <div>Failed to load image</div>
            <div style={{ fontSize: '12px', marginTop: '8px', color: '#999' }}>
              {src}
            </div>
          </div>
        )}
      </div>
      {showCaption && caption && (
        <div style={captionStyles}>{caption}</div>
      )}
    </div>
  );
};

export default ImageRenderer;
export { ImageRenderer };
