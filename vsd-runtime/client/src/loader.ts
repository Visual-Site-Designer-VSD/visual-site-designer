import { RendererRegistry } from './registry';
import type { Boot } from './types';

interface PluginBundle {
  pluginId?: string;
  renderers?: Record<string, any>;
  registerRenderers?: (registry: typeof RendererRegistry) => void;
}

/** "my-plugin-name" -> "MyPluginName": the global the plugin's IIFE bundle assigns itself to. */
export const globalNameFor = (pluginId: string): string =>
  pluginId.split('-').map((p) => p.charAt(0).toUpperCase() + p.slice(1)).join('');

function loadScript(src: string): Promise<void> {
  return new Promise((resolve, reject) => {
    const s = document.createElement('script');
    s.src = src;
    s.async = false;
    s.onload = () => resolve();
    s.onerror = () => reject(new Error(`Failed to load ${src}`));
    document.head.appendChild(s);
  });
}

/**
 * Loads the plugin bundles the published site pinned and registers their renderers.
 * Bundles are fetched with ?v=<pinned version>, so a changed plugin is always a new URL.
 */
export async function loadPlugins(boot: Boot): Promise<string[]> {
  const failed: string[] = [];
  await Promise.all(
    boot.plugins.map(async (p) => {
      try {
        await loadScript(`/_vsd/plugins/${encodeURIComponent(p.id)}/bundle.js?v=${encodeURIComponent(p.version)}`);
        const bundle = (window as unknown as Record<string, PluginBundle | undefined>)[globalNameFor(p.id)];
        if (!bundle) throw new Error(`bundle did not define window.${globalNameFor(p.id)}`);
        if (typeof bundle.registerRenderers === 'function') {
          bundle.registerRenderers(RendererRegistry);
        } else if (bundle.renderers) {
          for (const [id, renderer] of Object.entries(bundle.renderers)) {
            RendererRegistry.register(id, renderer, bundle.pluginId ?? p.id);
          }
        } else {
          throw new Error('bundle exports no renderers');
        }
      } catch (e) {
        console.error(`[vsd] plugin ${p.id} failed to load`, e);
        failed.push(p.id);
      }
    }),
  );
  return failed;
}
