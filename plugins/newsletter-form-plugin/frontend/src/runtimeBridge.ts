/**
 * Optional bridge to the VSD live runtime (window.VSD.runtime).
 *
 * With the runtime, a subscription is an event handled by this plugin's server-side handler and
 * the result lives in shared session state, so it survives reloads and shows in every open tab.
 * Without it (builder, previews) the form behaves as before.
 */

/** The shared state `newsletter`, declared in the runtime console. */
export interface Subscription {
  subscribed: boolean;
  email?: string;
  subscribedAt?: string;
}

export const SHARED_STATE = 'newsletter';

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
  useShared<T>(name: string, pluginId?: string): [T | undefined, (v: T) => Promise<Outcome>];
  emit(type: string, target: EmitTarget, payload?: Record<string, unknown>): Promise<Outcome>;
}

export const getRuntime = (): VsdRuntime | undefined =>
  (globalThis as unknown as { VSD?: { runtime?: VsdRuntime } }).VSD?.runtime;

const NOOP: [undefined, () => void] = [undefined, () => undefined];

/** The visitor's subscription from the shared state (undefined without a runtime or if it is not declared). */
export function useRuntimeSubscription(pluginId: string): Subscription | undefined {
  const rt = getRuntime();
  return rt?.useShared ? rt.useShared<Subscription>(SHARED_STATE, pluginId)[0] : NOOP[0];
}
