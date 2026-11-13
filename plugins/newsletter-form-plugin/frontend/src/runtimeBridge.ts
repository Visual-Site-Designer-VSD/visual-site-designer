/**
 * Optional bridge to the VSD live runtime (window.VSD.runtime).
 *
 * With the runtime, a subscription is an event handled by this plugin's server-side handler and
 * the result lives in shared session state, so it survives reloads and shows in every open tab.
 * Without it (builder, previews) the form behaves as before.
 */

export interface Subscription {
  email: string;
  subscribedAt: string;
}

export interface EmitTarget {
  pluginId: string;
  componentId?: string;
  instanceId?: string;
}

export interface Outcome {
  ok: boolean;
  message?: string;
  errors?: Record<string, string>;
}

interface VsdRuntime {
  useSharedState<T>(scope: 'SESSION' | 'PAGE' | 'PLUGIN' | 'COMPONENT', owner: string, key: string, initial?: T): [T | undefined, (v: T) => void];
  emit(type: string, target: EmitTarget, payload?: Record<string, unknown>): Promise<Outcome>;
}

export const getRuntime = (): VsdRuntime | undefined =>
  (globalThis as unknown as { VSD?: { runtime?: VsdRuntime } }).VSD?.runtime;

const NOOP: [undefined, () => void] = [undefined, () => undefined];

/** This plugin's subscription from session state (undefined when none, or without a runtime). */
export function useRuntimeSubscription(pluginId: string): Subscription | undefined {
  const rt = getRuntime();
  return rt ? rt.useSharedState<Subscription>('PLUGIN', pluginId, 'subscription')[0] : NOOP[0];
}
