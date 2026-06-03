/**
 * Plugin Renderer Registry.
 * Plugins register their React component renderers here via registerRenderers().
 */

export type RendererFn = React.ComponentType<any>;

const renderers: Record<string, RendererFn> = {};

export const RendererRegistry = {
  register(componentId: string, renderer: RendererFn) {
    renderers[componentId] = renderer;
  },

  get(componentId: string): RendererFn | undefined {
    return renderers[componentId];
  },

  has(componentId: string): boolean {
    return componentId in renderers;
  },

  getAll(): Record<string, RendererFn> {
    return { ...renderers };
  },
};

// Expose globally for plugin IIFEs
(window as any).__VSD_RENDERERS__ = renderers;
