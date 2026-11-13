import type { ComponentType } from 'react';

export type RendererComponent = ComponentType<any>;

/**
 * Same lookup rules as the builder's RendererRegistry (keys "pluginId:ComponentId", with a
 * generic "ComponentId" fallback and PascalCase normalisation). Container plugins find their
 * children through globalThis.RendererRegistry, so this object is exposed there.
 */
export class RendererRegistryClass {
  private renderers = new Map<string, RendererComponent>();

  register(componentId: string, renderer: RendererComponent, pluginId?: string): void {
    this.renderers.set(this.key(this.normalize(componentId), pluginId), renderer);
  }

  unregister(componentId: string, pluginId?: string): void {
    this.renderers.delete(this.key(this.normalize(componentId), pluginId));
  }

  get(componentId: string, pluginId?: string): RendererComponent | null {
    const id = this.normalize(componentId);
    if (pluginId) {
      const specific = this.renderers.get(this.key(id, pluginId));
      if (specific) return specific;
    }
    return this.renderers.get(this.key(id)) ?? null;
  }

  debugGetAllKeys(): string[] {
    return [...this.renderers.keys()];
  }

  private normalize(componentId: string): string {
    if (componentId.includes('-') || componentId.includes('_')) {
      return componentId
        .split(/[-_]/)
        .map((p) => p.charAt(0).toUpperCase() + p.slice(1).toLowerCase())
        .join('');
    }
    return componentId.charAt(0).toUpperCase() + componentId.slice(1);
  }

  private key(componentId: string, pluginId?: string): string {
    return pluginId ? `${pluginId}:${componentId}` : componentId;
  }
}

export const RendererRegistry = new RendererRegistryClass();
