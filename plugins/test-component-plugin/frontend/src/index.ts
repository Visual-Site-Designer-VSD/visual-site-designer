import type { PluginBundle, RendererComponent } from './types';
import TestComponentRenderer from './renderers/TestComponentRenderer';

export const PLUGIN_ID = 'test-component-plugin';

export const renderers: Record<string, RendererComponent> = {
  TestComponent: TestComponentRenderer,
};

export const pluginBundle: PluginBundle = {
  pluginId: PLUGIN_ID,
  renderers,
  version: '1.0.0',
};

export { TestComponentRenderer };
export default pluginBundle;
