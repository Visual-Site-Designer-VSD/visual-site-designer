import React from 'react';
import * as ReactDOM from 'react-dom';
import * as ReactDOMClient from 'react-dom/client';
import * as jsxRuntime from 'react/jsx-runtime';
import { App } from './App';
import { loadPlugins } from './loader';
import { RendererRegistry } from './registry';
import { Runtime } from './runtime';
import type { Boot } from './types';
import './runtime.css';

// Plugin bundles are IIFEs with react, react-dom and the JSX runtime as externals; they read
// these globals, so they share this single copy of React with the page.
const g = globalThis as unknown as Record<string, unknown>;
g.React = React;
g.ReactDOM = { ...ReactDOM, ...ReactDOMClient };
g.jsxRuntime = { jsx: jsxRuntime.jsx, jsxs: jsxRuntime.jsxs, Fragment: jsxRuntime.Fragment };
// Container plugins look their children up here.
g.RendererRegistry = RendererRegistry;

async function start() {
  const el = document.getElementById('vsd-boot');
  const root = document.getElementById('vsd-root');
  if (!el || !root) throw new Error('VSD boot data missing');
  const boot = JSON.parse(el.textContent ?? '{}') as Boot;

  const runtime = new Runtime(boot);
  window.VSD = { runtime, version: '1' };

  const failed = await loadPlugins(boot);
  if (failed.length) console.error('[vsd] plugins failed to load:', failed.join(', '));

  ReactDOMClient.createRoot(root).render(<App components={boot.page.definition.components ?? []} />);
  runtime.connect();
}

void start();
