/**
 * ExportTemplateRegistry - Central registry for component export templates
 *
 * This registry allows plugins to register custom export templates for their components,
 * ensuring that preview and export output remain consistent.
 *
 * Templates use mustache-style placeholders:
 * - {{propName}} - replaced with component prop value
 * - {{styles.cssProperty}} - replaced with specific style value
 * - {{styleString}} - replaced with complete inline style string
 * - {{children}} - replaced with rendered child content
 * - {{className}} - replaced with CSS class names
 */

import { ComponentInstance } from '../types/builder';

/**
 * Export template definition
 */
export interface ExportTemplate {
  /**
   * HTML template string with mustache-style placeholders
   * Example: '<hr style="{{styleString}}" class="horizontal-rule" />'
   */
  staticTemplate: string;

  /**
   * Thymeleaf template string (optional, falls back to staticTemplate)
   * Example: '<hr th:style="${styleString}" class="horizontal-rule" />'
   */
  thymeleafTemplate?: string;

  /**
   * Whether the component supports children
   */
  supportsChildren?: boolean;

  /**
   * Custom wrapper tag (defaults to 'div')
   */
  wrapperTag?: string;

  /**
   * Additional CSS classes to add
   */
  cssClasses?: string[];

  /**
   * Custom style generator function for complex styling logic
   */
  generateStyles?: (component: ComponentInstance) => string;

  /**
   * Custom content generator for complex content logic
   */
  generateContent?: (component: ComponentInstance, childrenHtml: string) => string;
}

/**
 * Registry key format: "pluginId:componentId" or just "componentId" for core components
 */
type TemplateKey = string;

/**
 * ExportTemplateRegistry class
 */
class ExportTemplateRegistryClass {
  private templates: Map<TemplateKey, ExportTemplate> = new Map();

  /**
   * Register an export template for a component
   *
   * @param componentId - The component ID (e.g., "HorizontalRow")
   * @param template - The export template definition
   * @param pluginId - Optional plugin ID for plugin-specific templates
   *
   * @example
   * ExportTemplateRegistry.register('HorizontalRow', {
   *   staticTemplate: '<hr style="{{styleString}}" />',
   *   thymeleafTemplate: '<hr th:style="${styleString}" />'
   * }, 'horizontal-row-plugin');
   */
  register(componentId: string, template: ExportTemplate, pluginId?: string): void {
    const normalizedComponentId = this.normalizeComponentId(componentId);
    const key = this.buildKey(normalizedComponentId, pluginId);
    this.templates.set(key, template);
    console.log(`[ExportTemplateRegistry] Registered template: ${key}`);
  }

  /**
   * Unregister an export template
   */
  unregister(componentId: string, pluginId?: string): void {
    const normalizedComponentId = this.normalizeComponentId(componentId);
    const key = this.buildKey(normalizedComponentId, pluginId);
    this.templates.delete(key);
    console.log(`[ExportTemplateRegistry] Unregistered template: ${key}`);
  }

  /**
   * Get an export template for a component
   * First checks for plugin-specific template, then falls back to generic template
   *
   * @param componentId - The component ID
   * @param pluginId - The plugin ID (from ComponentInstance)
   */
  get(componentId: string, pluginId?: string): ExportTemplate | null {
    const normalizedComponentId = this.normalizeComponentId(componentId);

    // First, try plugin-specific template
    if (pluginId) {
      const pluginKey = this.buildKey(normalizedComponentId, pluginId);
      const pluginTemplate = this.templates.get(pluginKey);
      if (pluginTemplate) {
        return pluginTemplate;
      }
    }

    // Fall back to generic template (just componentId)
    const genericKey = this.buildKey(normalizedComponentId);
    return this.templates.get(genericKey) || null;
  }

  /**
   * Check if a template exists
   */
  has(componentId: string, pluginId?: string): boolean {
    return this.get(componentId, pluginId) !== null;
  }

  /**
   * Render a static HTML export using the template
   *
   * @param component - The component instance
   * @param childrenHtml - Rendered HTML of children (if any)
   */
  renderStatic(component: ComponentInstance, childrenHtml: string = ''): string | null {
    const template = this.get(component.componentId, component.pluginId);
    if (!template) {
      return null;
    }

    // Use custom content generator if provided
    if (template.generateContent) {
      return template.generateContent(component, childrenHtml);
    }

    // Generate style string
    const styleString = template.generateStyles
      ? template.generateStyles(component)
      : this.generateStyleString(component);

    // Build class names
    const classNames = [
      `component-${component.componentId.toLowerCase()}`,
      ...(template.cssClasses || []),
      ...(component.props?.className ? [component.props.className] : [])
    ].join(' ');

    // Render template with placeholders
    let html = template.staticTemplate;
    html = this.replacePlaceholders(html, component, styleString, classNames, childrenHtml);

    return html;
  }

  /**
   * Render a Thymeleaf export using the template
   *
   * @param component - The component instance
   * @param childrenHtml - Rendered HTML of children (if any)
   */
  renderThymeleaf(component: ComponentInstance, childrenHtml: string = ''): string | null {
    const template = this.get(component.componentId, component.pluginId);
    if (!template) {
      return null;
    }

    // Use thymeleaf template if available, otherwise fall back to static
    const templateString = template.thymeleafTemplate || template.staticTemplate;

    // Use custom content generator if provided
    if (template.generateContent) {
      return template.generateContent(component, childrenHtml);
    }

    // Generate style string
    const styleString = template.generateStyles
      ? template.generateStyles(component)
      : this.generateStyleString(component);

    // Build class names
    const classNames = [
      `component-${component.componentId.toLowerCase()}`,
      ...(template.cssClasses || []),
      ...(component.props?.className ? [component.props.className] : [])
    ].join(' ');

    // Render template with placeholders
    let html = templateString;
    html = this.replacePlaceholders(html, component, styleString, classNames, childrenHtml);

    return html;
  }

  /**
   * Replace mustache-style placeholders in template
   */
  private replacePlaceholders(
    template: string,
    component: ComponentInstance,
    styleString: string,
    classNames: string,
    childrenHtml: string
  ): string {
    let result = template;

    // Replace special placeholders
    result = result.replace(/\{\{styleString\}\}/g, styleString);
    result = result.replace(/\{\{className\}\}/g, classNames);
    result = result.replace(/\{\{children\}\}/g, childrenHtml);
    result = result.replace(/\{\{componentId\}\}/g, component.componentId);
    result = result.replace(/\{\{id\}\}/g, component.id);

    // Replace prop placeholders: {{propName}}
    if (component.props) {
      for (const [key, value] of Object.entries(component.props)) {
        const placeholder = new RegExp(`\\{\\{${key}\\}\\}`, 'g');
        const stringValue = value !== undefined && value !== null ? String(value) : '';
        result = result.replace(placeholder, stringValue);
      }
    }

    // Replace style placeholders: {{styles.cssProperty}}
    if (component.styles) {
      for (const [key, value] of Object.entries(component.styles)) {
        const placeholder = new RegExp(`\\{\\{styles\\.${key}\\}\\}`, 'g');
        const stringValue = value !== undefined && value !== null ? String(value) : '';
        result = result.replace(placeholder, stringValue);
      }
    }

    // Clean up any remaining unmatched placeholders
    result = result.replace(/\{\{[^}]+\}\}/g, '');

    return result;
  }

  /**
   * Generate inline style string from component styles
   */
  private generateStyleString(component: ComponentInstance): string {
    if (!component.styles) {
      return '';
    }

    const styleEntries: string[] = [];

    for (const [key, value] of Object.entries(component.styles)) {
      if (value !== undefined && value !== null && value !== '') {
        // Convert camelCase to kebab-case for CSS
        const cssKey = key.replace(/([A-Z])/g, '-$1').toLowerCase();
        styleEntries.push(`${cssKey}: ${value}`);
      }
    }

    return styleEntries.join('; ');
  }

  /**
   * Normalize componentId for case-insensitive lookup
   */
  private normalizeComponentId(componentId: string): string {
    if (componentId.includes('-') || componentId.includes('_')) {
      return componentId
        .split(/[-_]/)
        .map(part => part.charAt(0).toUpperCase() + part.slice(1).toLowerCase())
        .join('');
    }
    return componentId.charAt(0).toUpperCase() + componentId.slice(1);
  }

  /**
   * Build a registry key from componentId and optional pluginId
   */
  private buildKey(componentId: string, pluginId?: string): TemplateKey {
    return pluginId ? `${pluginId}:${componentId}` : componentId;
  }

  /**
   * Get all registered template keys (for debugging)
   */
  getRegisteredKeys(): string[] {
    return Array.from(this.templates.keys());
  }

  /**
   * Clear all registered templates (useful for testing)
   */
  clear(): void {
    this.templates.clear();
  }
}

// Export singleton instance
export const ExportTemplateRegistry = new ExportTemplateRegistryClass();

// Also export the class for testing
export { ExportTemplateRegistryClass };
