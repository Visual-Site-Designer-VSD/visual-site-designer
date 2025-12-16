package dev.mainul35.cms.sdk.component;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Size constraints for a UI component in the visual builder.
 * Controls how the component can be resized.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SizeConstraints {

    /**
     * Whether the component can be resized
     */
    private boolean resizable;

    /**
     * Default width (e.g., "300px", "50%", "auto")
     */
    private String defaultWidth;

    /**
     * Default height (e.g., "200px", "100%", "auto")
     */
    private String defaultHeight;

    /**
     * Minimum width (e.g., "100px", "10%")
     */
    private String minWidth;

    /**
     * Maximum width (e.g., "1200px", "100%")
     */
    private String maxWidth;

    /**
     * Minimum height (e.g., "50px")
     */
    private String minHeight;

    /**
     * Maximum height (e.g., "800px", "none")
     */
    private String maxHeight;

    /**
     * Whether width is locked (can't be changed)
     */
    private boolean widthLocked;

    /**
     * Whether height is locked (can't be changed)
     */
    private boolean heightLocked;

    /**
     * Maintain aspect ratio when resizing
     */
    private boolean maintainAspectRatio;

    /**
     * Aspect ratio (width/height)
     */
    private Double aspectRatio;
}
