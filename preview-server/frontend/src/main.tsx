import React from 'react';
import * as ReactDOM from 'react-dom';
import { createRoot } from 'react-dom/client';
import { jsx, jsxs, Fragment } from 'react/jsx-runtime';
import App from './App';
import { RendererRegistry } from './services/RendererRegistry';
import { PluginContextRegistryContext } from './components/ContextProviderTree';

// Expose globals for plugin IIFE bundles
(window as any).React = React;
(window as any).ReactDOM = ReactDOM;
(window as any).jsxRuntime = { jsx, jsxs, Fragment };

// VSD API for plugins
(window as any).__VSD__ = {
  PluginContextRegistryContext,
  RendererRegistry,
  registerPluginContextValue(contextId: string, value: any) {
    if ((window as any).__VSD__._registerFn) {
      (window as any).__VSD__._registerFn(contextId, value);
    }
  },
  unregisterPluginContextValue(contextId: string) {
    if ((window as any).__VSD__._unregisterFn) {
      (window as any).__VSD__._unregisterFn(contextId);
    }
  },
};

// Mount
createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <App />
  </React.StrictMode>
);
