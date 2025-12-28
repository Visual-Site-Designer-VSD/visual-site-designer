/**
 * Auth Component Plugin - Frontend Bundle
 *
 * This is the main entry point for the auth component plugin's frontend code.
 * It exports all renderers in a format that the main application can dynamically load.
 */

import type { PluginBundle, RendererComponent } from './types';

// Import all renderers
import LoginFormRenderer from './renderers/LoginFormRenderer';
import RegisterFormRenderer from './renderers/RegisterFormRenderer';
import ForgotPasswordFormRenderer from './renderers/ForgotPasswordFormRenderer';
import LogoutButtonRenderer from './renderers/LogoutButtonRenderer';
import SocialLoginButtonsRenderer from './renderers/SocialLoginButtonsRenderer';

// Import styles (will be extracted to bundle.css by Vite)
import './styles/AuthComponents.css';

/**
 * Plugin ID must match the Java plugin's ID
 */
export const PLUGIN_ID = 'auth-component-plugin';

/**
 * Map of component IDs to their renderer components
 */
export const renderers: Record<string, RendererComponent> = {
  LoginForm: LoginFormRenderer,
  RegisterForm: RegisterFormRenderer,
  ForgotPasswordForm: ForgotPasswordFormRenderer,
  LogoutButton: LogoutButtonRenderer,
  SocialLoginButtons: SocialLoginButtonsRenderer,
};

/**
 * Plugin bundle export - used by the dynamic loader
 */
export const pluginBundle: PluginBundle = {
  pluginId: PLUGIN_ID,
  renderers,
  version: '1.0.0',
};

// Also export individual renderers for direct imports
export {
  LoginFormRenderer,
  RegisterFormRenderer,
  ForgotPasswordFormRenderer,
  LogoutButtonRenderer,
  SocialLoginButtonsRenderer,
};

// Default export is the plugin bundle
export default pluginBundle;

/**
 * Self-registration function
 * This can be called by the host application after loading the bundle
 */
export function registerRenderers(registry: {
  register: (componentId: string, renderer: RendererComponent, pluginId?: string) => void;
}): void {
  Object.entries(renderers).forEach(([componentId, renderer]) => {
    registry.register(componentId, renderer, PLUGIN_ID);
  });
  console.log(`[${PLUGIN_ID}] Registered ${Object.keys(renderers).length} renderers`);
}
