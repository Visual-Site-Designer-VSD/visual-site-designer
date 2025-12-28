/**
 * Plugin Loader Service
 *
 * This service handles loading plugin frontend bundles from the backend API.
 * When a plugin is loaded, its renderers are automatically registered
 * with the RendererRegistry.
 */

import { RendererRegistry, RendererComponent } from '../components/builder/renderers/RendererRegistry';

/**
 * Plugin bundle structure expected from dynamically loaded plugins
 */
interface PluginBundle {
  pluginId: string;
  renderers: Record<string, RendererComponent>;
  version?: string;
  registerRenderers?: (registry: typeof RendererRegistry) => void;
}

/**
 * Plugin frontend info from backend API
 */
interface PluginFrontendInfo {
  pluginId: string;
  hasBundleJs: boolean;
  hasBundleCss: boolean;
}

/**
 * Tracks which plugins have been loaded
 */
const loadedPlugins = new Set<string>();
const loadingPlugins = new Map<string, Promise<boolean>>();

/**
 * Base URL for plugin assets API
 */
const PLUGIN_API_BASE = '/api/plugins';

/**
 * Check if a plugin has frontend assets available
 */
export async function checkPluginFrontend(pluginId: string): Promise<PluginFrontendInfo | null> {
  try {
    const response = await fetch(`${PLUGIN_API_BASE}/${pluginId}/has-frontend`);
    if (!response.ok) {
      return null;
    }
    return await response.json();
  } catch (error) {
    console.warn(`[PluginLoader] Failed to check frontend for ${pluginId}:`, error);
    return null;
  }
}

/**
 * Load a plugin's CSS styles
 */
async function loadPluginStyles(pluginId: string): Promise<void> {
  const styleId = `plugin-styles-${pluginId}`;

  // Check if already loaded
  if (document.getElementById(styleId)) {
    return;
  }

  const link = document.createElement('link');
  link.id = styleId;
  link.rel = 'stylesheet';
  link.href = `${PLUGIN_API_BASE}/${pluginId}/bundle.css`;

  return new Promise((resolve, reject) => {
    link.onload = () => {
      console.log(`[PluginLoader] Loaded styles for ${pluginId}`);
      resolve();
    };
    link.onerror = () => {
      // CSS is optional, don't fail if not found
      console.debug(`[PluginLoader] No styles found for ${pluginId}`);
      resolve();
    };
    document.head.appendChild(link);
  });
}

/**
 * Load a plugin's JavaScript bundle and register its renderers
 */
async function loadPluginBundle(pluginId: string): Promise<boolean> {
  try {
    const bundleUrl = `${PLUGIN_API_BASE}/${pluginId}/bundle.js`;

    // Dynamic import of the plugin bundle
    const module = await import(/* @vite-ignore */ bundleUrl);

    // Handle different export formats
    const pluginBundle: PluginBundle = module.default || module.pluginBundle || module;

    if (!pluginBundle.renderers && !pluginBundle.registerRenderers) {
      console.warn(`[PluginLoader] Invalid bundle format for ${pluginId}: no renderers found`);
      return false;
    }

    // If the plugin exports a registerRenderers function, use it
    if (typeof pluginBundle.registerRenderers === 'function') {
      pluginBundle.registerRenderers(RendererRegistry);
    }
    // Otherwise, register renderers manually
    else if (pluginBundle.renderers) {
      Object.entries(pluginBundle.renderers).forEach(([componentId, renderer]) => {
        RendererRegistry.register(componentId, renderer, pluginBundle.pluginId || pluginId);
      });
    }

    console.log(
      `[PluginLoader] Loaded bundle for ${pluginId}:`,
      Object.keys(pluginBundle.renderers || {})
    );

    return true;
  } catch (error) {
    console.error(`[PluginLoader] Failed to load bundle for ${pluginId}:`, error);
    return false;
  }
}

/**
 * Load a plugin's frontend assets (both JS and CSS)
 *
 * @param pluginId The plugin ID to load
 * @returns true if the plugin was loaded successfully
 */
export async function loadPlugin(pluginId: string): Promise<boolean> {
  // Check if already loaded
  if (loadedPlugins.has(pluginId)) {
    return true;
  }

  // Check if currently loading
  if (loadingPlugins.has(pluginId)) {
    return loadingPlugins.get(pluginId)!;
  }

  // Start loading
  const loadPromise = (async () => {
    try {
      // First check if the plugin has frontend assets
      const frontendInfo = await checkPluginFrontend(pluginId);

      if (!frontendInfo || !frontendInfo.hasBundleJs) {
        console.debug(`[PluginLoader] Plugin ${pluginId} has no frontend bundle`);
        return false;
      }

      // Load CSS first (parallel with JS would cause FOUC)
      if (frontendInfo.hasBundleCss) {
        await loadPluginStyles(pluginId);
      }

      // Load JavaScript bundle
      const success = await loadPluginBundle(pluginId);

      if (success) {
        loadedPlugins.add(pluginId);
      }

      return success;
    } finally {
      loadingPlugins.delete(pluginId);
    }
  })();

  loadingPlugins.set(pluginId, loadPromise);
  return loadPromise;
}

/**
 * Load multiple plugins in parallel
 *
 * @param pluginIds Array of plugin IDs to load
 * @returns Map of pluginId to success status
 */
export async function loadPlugins(pluginIds: string[]): Promise<Map<string, boolean>> {
  const results = new Map<string, boolean>();

  await Promise.all(
    pluginIds.map(async (pluginId) => {
      const success = await loadPlugin(pluginId);
      results.set(pluginId, success);
    })
  );

  return results;
}

/**
 * Unload a plugin's frontend assets
 *
 * @param pluginId The plugin ID to unload
 */
export function unloadPlugin(pluginId: string): void {
  // Remove styles
  const styleElement = document.getElementById(`plugin-styles-${pluginId}`);
  if (styleElement) {
    styleElement.remove();
  }

  // Note: JavaScript modules cannot be fully unloaded from memory
  // We just unregister the renderers
  // The actual module will be garbage collected if no references remain

  loadedPlugins.delete(pluginId);

  console.log(`[PluginLoader] Unloaded plugin ${pluginId}`);
}

/**
 * Check if a plugin is loaded
 */
export function isPluginLoaded(pluginId: string): boolean {
  return loadedPlugins.has(pluginId);
}

/**
 * Get list of loaded plugins
 */
export function getLoadedPlugins(): string[] {
  return Array.from(loadedPlugins);
}

/**
 * Preload plugins that are likely to be needed
 * This can be called early to reduce loading delays later
 *
 * @param pluginIds Plugin IDs to preload
 */
export function preloadPlugins(pluginIds: string[]): void {
  pluginIds.forEach((pluginId) => {
    if (!loadedPlugins.has(pluginId) && !loadingPlugins.has(pluginId)) {
      // Start loading but don't wait for it
      loadPlugin(pluginId).catch(() => {
        // Ignore preload failures
      });
    }
  });
}

export default {
  loadPlugin,
  loadPlugins,
  unloadPlugin,
  isPluginLoaded,
  getLoadedPlugins,
  preloadPlugins,
  checkPluginFrontend,
};
