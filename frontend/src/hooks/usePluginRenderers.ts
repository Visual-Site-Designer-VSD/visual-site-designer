import React, { useEffect, useRef } from 'react';
import { useComponentStore } from '../stores/componentStore';
import { RendererRegistry, RendererProps } from '../components/builder/renderers/RendererRegistry';
import { loadPlugin, isPluginLoaded } from '../services/pluginLoaderService';

/**
 * Hook to automatically load plugin renderers when components are registered
 *
 * This hook watches the component store and attempts to load renderers
 * for any plugin components. It supports two loading mechanisms:
 *
 * 1. Plugin bundles from backend API (preferred for self-contained plugins)
 *    - Loads from /api/plugins/{pluginId}/bundle.js
 *
 * 2. Legacy reactBundlePath (for backwards compatibility)
 *    - Uses the bundle URL specified in the component manifest
 *
 * Usage in your app:
 * ```tsx
 * function App() {
 *   usePluginRenderers(); // Call once at app level
 *   return <BuilderPage />;
 * }
 * ```
 */
export function usePluginRenderers(): void {
  const components = useComponentStore((state) => state.components);
  const loadedPluginsRef = useRef<Set<string>>(new Set());

  useEffect(() => {
    // Group components by plugin ID
    const pluginComponents = new Map<string, typeof components>();

    components.forEach((entry) => {
      if (!entry.isActive) return;

      // Skip core components
      if (entry.pluginId === 'core' || !entry.pluginId) return;

      // Skip if renderer already registered
      if (RendererRegistry.has(entry.componentId, entry.pluginId)) return;

      // Group by plugin
      if (!pluginComponents.has(entry.pluginId)) {
        pluginComponents.set(entry.pluginId, []);
      }
      pluginComponents.get(entry.pluginId)!.push(entry);
    });

    // Load each plugin's bundle
    pluginComponents.forEach(async (entries, pluginId) => {
      // Skip if already attempted loading
      if (loadedPluginsRef.current.has(pluginId)) return;
      loadedPluginsRef.current.add(pluginId);

      // First, try to load from backend API (self-contained plugin)
      if (!isPluginLoaded(pluginId)) {
        const loaded = await loadPlugin(pluginId);
        if (loaded) {
          console.log(`[usePluginRenderers] Loaded plugin from API: ${pluginId}`);
          return;
        }
      }

      // Fallback: Try loading from reactBundlePath if specified
      for (const entry of entries) {
        if (!entry.reactBundlePath) continue;

        if (RendererRegistry.has(entry.componentId, entry.pluginId)) continue;

        try {
          await RendererRegistry.loadFromBundle(
            entry.componentId,
            entry.reactBundlePath,
            entry.pluginId
          );
        } catch (error) {
          console.warn(
            `[usePluginRenderers] Failed to load renderer for ${entry.pluginId}:${entry.componentId}`,
            error
          );
        }
      }
    });
  }, [components]);
}

/**
 * Create a renderer wrapper that adapts a plugin's component to the RendererProps interface
 *
 * Plugin components typically receive props directly (text, variant, etc.)
 * but core renderers receive { component, isEditMode }
 *
 * This utility creates an adapter that bridges the two interfaces.
 *
 * @example
 * // In your plugin's registration code:
 * import Button from './Button'; // Your plugin's Button component
 *
 * const ButtonRenderer = createPluginRenderer(Button);
 * RendererRegistry.register('Button', ButtonRenderer, 'my-plugin');
 */
export function createPluginRenderer(
  PluginComponent: React.ComponentType<any>
): React.FC<RendererProps> {
  const PluginRenderer: React.FC<RendererProps> = ({ component, isEditMode }) => {
    // Merge props and styles for the plugin component
    const pluginProps = {
      ...component.props,
      styles: component.styles,
      isEditMode,
    };

    return React.createElement(PluginComponent, pluginProps);
  };

  return PluginRenderer;
}

/**
 * Register a plugin renderer manually
 *
 * This is the main API for plugin developers to register their renderers.
 * Call this in your plugin's initialization code.
 *
 * @example
 * // In your plugin's entry point:
 * import { registerPluginRenderer } from '@dynamic-site-builder/core';
 * import MyCustomButton from './components/MyCustomButton';
 *
 * registerPluginRenderer('Button', MyCustomButton, 'my-custom-plugin');
 */
export function registerPluginRenderer(
  componentId: string,
  Component: React.ComponentType<any>,
  pluginId: string
): void {
  const Renderer = createPluginRenderer(Component);
  RendererRegistry.register(componentId, Renderer, pluginId);
}

/**
 * Unregister a plugin renderer
 *
 * Call this when your plugin is being unloaded/disabled.
 */
export function unregisterPluginRenderer(componentId: string, pluginId: string): void {
  RendererRegistry.unregister(componentId, pluginId);
}

export default usePluginRenderers;
