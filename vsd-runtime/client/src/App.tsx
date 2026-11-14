import React, { useEffect, useRef, useState, useSyncExternalStore } from 'react';
import { RendererRegistry } from './registry';
import type { Runtime } from './runtime';
import type { ComponentInstance } from './types';

/** A broken plugin must not take the whole page down. */
class Boundary extends React.Component<{ id: string; children: React.ReactNode }, { failed: boolean }> {
  state = { failed: false };

  static getDerivedStateFromError() {
    return { failed: true };
  }

  componentDidCatch(error: unknown) {
    console.error(`[vsd] component ${this.props.id} crashed`, error);
  }

  render() {
    return this.state.failed ? <div className="vsd-component-error" data-component={this.props.id} /> : this.props.children;
  }
}

function Root({ component }: { component: ComponentInstance }) {
  if (component.isVisible === false) return null;
  const Renderer = RendererRegistry.get(component.componentId, component.pluginId);
  if (!Renderer) {
    console.warn(`[vsd] no renderer for ${component.pluginId}:${component.componentId}`);
    return null;
  }
  return (
    <Boundary id={component.instanceId}>
      <div className="vsd-root-component" style={{ width: '100%' }}>
        <Renderer component={component} isEditMode={false} />
      </div>
    </Boundary>
  );
}

/**
 * Bindings such as {{auth.user.displayName}} and {{shared.cart.total}} are filled in on the server for one
 * visitor, so when the signed-in user or a shared state the page reads changes, the page is fetched again.
 * A late answer to an older request is ignored.
 */
export function App({ components: initial, runtime }: { components: ComponentInstance[]; runtime: Runtime }) {
  const [components, setComponents] = useState(initial);
  const [sharedUsed, setSharedUsed] = useState<string[]>(runtime.boot.page.sharedUsed ?? []);
  const { user } = runtime.useAuth();
  const userId = user?.id ?? null;
  // Bindings such as {{shared.cart.total}} read the visitor's shared state, so watch the values the page uses.
  const sharedKey = useSyncExternalStore(runtime.store.subscribe, () =>
    sharedUsed.map((n) => JSON.stringify(runtime.getShared(n)) ?? '').join(''),
  );
  const firstRender = useRef(true);
  const latest = useRef(0);

  useEffect(() => {
    if (firstRender.current) {
      firstRender.current = false;
      return;
    }
    const mine = ++latest.current;
    void runtime.fetchPage().then((next) => {
      if (next && mine === latest.current) {
        setComponents(next.components);
        setSharedUsed((prev) => (prev.join() === next.sharedUsed.join() ? prev : next.sharedUsed));
      }
    });
  }, [userId, sharedKey, runtime]);

  const ordered = [...components].sort((a, b) => (a.displayOrder ?? 0) - (b.displayOrder ?? 0));
  return (
    <main className="vsd-page">
      {ordered.map((c) => (
        <Root key={c.instanceId} component={c} />
      ))}
    </main>
  );
}
