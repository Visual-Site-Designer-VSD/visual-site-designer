package dev.mainul35.cms.sdk;

import dev.mainul35.cms.sdk.component.ComponentManifest;
import dev.mainul35.cms.sdk.component.ValidationResult;

import java.util.List;
import java.util.Map;

/**
 * Interface for UI component plugins.
 * Extends the base Plugin interface with UI component-specific methods.
 *
 * UI component plugins provide React components that can be used in the visual site builder.
 */
public interface UIComponentPlugin extends Plugin {

    /**
     * Get the component manifest containing all metadata, props, and styles.
     *
     * @return ComponentManifest with complete component configuration
     */
    ComponentManifest getComponentManifest();

    /**
     * Get all component manifests provided by this plugin.
     * Override this for plugins that provide multiple component variants
     * (e.g., navbar with 8 variants, auth with 5 forms).
     * Default implementation returns a singleton list of {@link #getComponentManifest()}.
     *
     * @return List of all component manifests from this plugin
     */
    default List<ComponentManifest> getComponentManifests() {
        ComponentManifest manifest = getComponentManifest();
        return manifest != null ? List.of(manifest) : List.of();
    }

    /**
     * Get the path to the React component bundle (JS/JSX file).
     * This path is relative to the plugin resources directory.
     *
     * @return Path to React component (e.g., "/components/Button.jsx")
     */
    String getReactComponentPath();

    /**
     * Get the component preview thumbnail image.
     * This is shown in the component palette for visual identification.
     *
     * @return Thumbnail image as byte array, or null if not available
     */
    byte[] getComponentThumbnail();

    /**
     * Validate component props before rendering.
     * This allows custom validation logic beyond basic type checking.
     *
     * @param props Component properties to validate
     * @return ValidationResult indicating if props are valid
     */
    ValidationResult validateProps(Map<String, Object> props);

    /**
     * Server-side rendering support (optional).
     * Renders the component to HTML on the server.
     * This is useful for SEO and initial page load performance.
     *
     * @param props Component properties
     * @param styles Component CSS styles
     * @return HTML string representation of the component
     */
    default String renderToHTML(Map<String, Object> props, Map<String, String> styles) {
        // Default implementation returns null (client-side rendering only)
        return null;
    }

    /**
     * Get component configuration schema for advanced validation.
     * Returns a JSON schema describing the component's props structure.
     *
     * @return JSON schema string, or null if not provided
     */
    default String getPropsSchema() {
        return null;
    }

}
