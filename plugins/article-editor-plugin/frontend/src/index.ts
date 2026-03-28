/**
 * Article Editor Plugin - Frontend Bundle
 *
 * Includes TipTap rich text editor bundled in the IIFE.
 */

import type { PluginBundle, RendererComponent } from './types';
import ArticleEditorRenderer from './renderers/ArticleEditorRenderer';
import './styles/ArticleEditor.css';

export const PLUGIN_ID = 'article-editor-plugin';

export const renderers: Record<string, RendererComponent> = {
  ArticleEditor: ArticleEditorRenderer,
};

export const pluginBundle: PluginBundle = {
  pluginId: PLUGIN_ID,
  renderers,
  version: '1.0.0',
};

export { ArticleEditorRenderer };

export default pluginBundle;

export function registerRenderers(registry: {
  register: (componentId: string, renderer: RendererComponent, pluginId?: string) => void;
}): void {
  Object.entries(renderers).forEach(([componentId, renderer]) => {
    registry.register(componentId, renderer, PLUGIN_ID);
  });
  console.log(`[${PLUGIN_ID}] Registered ${Object.keys(renderers).length} renderers`);
}
