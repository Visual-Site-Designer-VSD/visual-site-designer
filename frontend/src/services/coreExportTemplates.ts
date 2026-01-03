/**
 * Core Export Templates - Registers export templates for built-in and known plugin components
 *
 * This file registers export templates for components that don't have switch-case handlers
 * in the export services, ensuring consistent preview and export output.
 */

import { ExportTemplateRegistry, ExportTemplate } from './ExportTemplateRegistry';
import { ComponentInstance } from '../types/builder';

/**
 * Generate inline style string from component
 */
function generateStyleString(component: ComponentInstance): string {
  if (!component.styles) return '';

  const styleEntries: string[] = [];
  for (const [key, value] of Object.entries(component.styles)) {
    if (value !== undefined && value !== null && value !== '') {
      const cssKey = key.replace(/([A-Z])/g, '-$1').toLowerCase();
      styleEntries.push(`${cssKey}: ${value}`);
    }
  }
  return styleEntries.join('; ');
}

/**
 * HorizontalRow Export Template
 *
 * Replicates the exact HTML structure from HorizontalRowRenderer.tsx:
 * - Container div with flexbox alignment
 * - HR element with border-top styling
 */
const horizontalRowTemplate: ExportTemplate = {
  staticTemplate: '', // We use custom content generator instead
  supportsChildren: false,

  generateContent: (component: ComponentInstance): string => {
    const props = component.props || {};
    const styles = component.styles || {};

    // Extract props with defaults (matching HorizontalRowRenderer lines 23-26)
    const thickness = (props.thickness as string) || '2px';
    const lineStyle = (props.lineStyle as string) || 'solid';
    const width = (props.width as string) || '100%';
    const alignment = (props.alignment as string) || 'center';

    // Extract styles with defaults (matching HorizontalRowRenderer lines 29-31)
    const color = (styles.color as string) || '#e0e0e0';
    const marginTop = (styles.marginTop as string) || '16px';
    const marginBottom = (styles.marginBottom as string) || '16px';

    // Map alignment to flexbox (matching lines 34-41)
    const getJustifyContent = (): string => {
      switch (alignment) {
        case 'left': return 'flex-start';
        case 'right': return 'flex-end';
        case 'center':
        default: return 'center';
      }
    };

    // Container styles (matching lines 43-51)
    const containerStyle = [
      'width: 100%',
      'display: flex',
      `justify-content: ${getJustifyContent()}`,
      'align-items: center',
      'box-sizing: border-box',
      `margin-top: ${marginTop}`,
      `margin-bottom: ${marginBottom}`,
    ].join('; ');

    // HR styles (matching lines 53-59)
    const hrStyle = [
      `width: ${width}`,
      'height: 0',
      'border: none',
      `border-top: ${thickness} ${lineStyle} ${color}`,
      'margin: 0',
    ].join('; ');

    return `<div style="${containerStyle}" class="horizontal-row-container"><hr style="${hrStyle}" /></div>`;
  },
};

/**
 * Register all core export templates
 * Call this function during application initialization
 */
export function registerCoreExportTemplates(): void {
  console.log('[CoreExportTemplates] Registering core export templates...');

  // Register HorizontalRow template
  ExportTemplateRegistry.register('HorizontalRow', horizontalRowTemplate, 'horizontal-row-plugin');
  // Also register without plugin ID for fallback
  ExportTemplateRegistry.register('HorizontalRow', horizontalRowTemplate);

  console.log('[CoreExportTemplates] Registered templates:', ExportTemplateRegistry.getRegisteredKeys());
}

/**
 * Register export template for a specific plugin component
 * This is called by the plugin loader when loading plugin manifests
 */
export function registerPluginExportTemplate(
  componentId: string,
  pluginId: string,
  staticTemplate?: string,
  thymeleafTemplate?: string,
  exportMetadata?: Record<string, any>
): void {
  if (!staticTemplate && !thymeleafTemplate) {
    console.log(`[CoreExportTemplates] No export template provided for ${pluginId}:${componentId}`);
    return;
  }

  const template: ExportTemplate = {
    staticTemplate: staticTemplate || '',
    thymeleafTemplate: thymeleafTemplate,
    supportsChildren: exportMetadata?.supportsChildren || false,
    wrapperTag: exportMetadata?.wrapperTag,
    cssClasses: exportMetadata?.cssClasses,
  };

  ExportTemplateRegistry.register(componentId, template, pluginId);
  console.log(`[CoreExportTemplates] Registered export template for ${pluginId}:${componentId}`);
}
