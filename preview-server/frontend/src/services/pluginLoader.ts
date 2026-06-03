/**
 * Plugin Loader Service.
 * Loads plugin IIFE bundles and CSS from /plugins/{pluginId}/.
 */

const loadedPlugins = new Set<string>();

export async function loadPlugin(pluginId: string): Promise<void> {
  if (loadedPlugins.has(pluginId)) return;

  // Load CSS if available
  try {
    const cssUrl = `/plugins/${pluginId}/bundle.css`;
    const cssRes = await fetch(cssUrl, { method: 'HEAD' });
    if (cssRes.ok) {
      const link = document.createElement('link');
      link.rel = 'stylesheet';
      link.href = cssUrl;
      document.head.appendChild(link);
    }
  } catch {
    // CSS is optional
  }

  // Load JS bundle
  return new Promise<void>((resolve, reject) => {
    const script = document.createElement('script');
    script.src = `/plugins/${pluginId}/bundle.js`;
    script.onload = () => {
      loadedPlugins.add(pluginId);
      console.log(`[PluginLoader] Loaded plugin: ${pluginId}`);
      resolve();
    };
    script.onerror = () => {
      console.error(`[PluginLoader] Failed to load plugin: ${pluginId}`);
      reject(new Error(`Failed to load plugin: ${pluginId}`));
    };
    document.body.appendChild(script);
  });
}

export async function loadPlugins(pluginIds: string[]): Promise<void> {
  await Promise.allSettled(pluginIds.map(loadPlugin));
}
