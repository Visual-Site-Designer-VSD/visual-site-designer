/**
 * Page Layout Plugin - Frontend Bundle
 *
 * Provides a structured page layout component with 5 distinct regions:
 * - Header (top, full width)
 * - Footer (bottom, full width)
 * - Left Panel (left sidebar)
 * - Right Panel (right sidebar)
 * - Center/Main Content (center area)
 */

import type { PluginBundle, RendererComponent } from './types';
import PageLayoutRenderer from './renderers/PageLayoutRenderer';

export const PLUGIN_ID = 'page-layout-plugin';

export const renderers: Record<string, RendererComponent> = {
  PageLayout: PageLayoutRenderer,
};

export const pluginBundle: PluginBundle = {
  pluginId: PLUGIN_ID,
  renderers,
  version: '1.0.0',
};

export { PageLayoutRenderer };

export default pluginBundle;

export function registerRenderers(registry: {
  register: (componentId: string, renderer: RendererComponent, pluginId?: string) => void;
}): void {
  Object.entries(renderers).forEach(([componentId, renderer]) => {
    registry.register(componentId, renderer, PLUGIN_ID);
  });
  console.log(`[${PLUGIN_ID}] Registered ${Object.keys(renderers).length} renderers`);
}
