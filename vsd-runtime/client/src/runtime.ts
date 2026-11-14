import { useCallback, useSyncExternalStore } from 'react';
import { StateStore } from './store';
import type { Boot, ComponentInstance, EventOutcome, RuntimeUser, Scope, WirePatch } from './types';

const newId = (): string =>
  typeof crypto !== 'undefined' && 'randomUUID' in crypto
    ? crypto.randomUUID()
    : Array.from({ length: 4 }, () => Math.random().toString(16).slice(2, 10)).join('');

export interface EmitTarget {
  pluginId: string;
  componentId?: string;
  instanceId?: string;
}

/**
 * The API plugins use to take part in the live site. It is published as window.VSD.runtime.
 * Plugins must treat it as optional: in the builder (and in tests) window.VSD is absent and
 * they fall back to their local behaviour.
 */
export class Runtime {
  readonly isRuntime = true;
  readonly store = new StateStore();
  private csrf: string;
  private stream: EventSource | null = null;
  private reconnectDelay = 1000;
  private refreshing: Promise<void> | null = null;

  constructor(readonly boot: Boot) {
    this.csrf = boot.session.csrf;
    this.store.replace(boot.session.entries, boot.session.rev);
  }

  get page(): string {
    return this.boot.page.name;
  }

  get site(): Boot['site'] {
    return this.boot.site;
  }

  get pages(): Boot['pages'] {
    return this.boot.pages;
  }

  // ---- state --------------------------------------------------------------------------------

  getState<T = unknown>(scope: Scope, owner: string, key: string): T | undefined {
    return this.store.get(scope, owner, key) as T | undefined;
  }

  /** Writes shared state: shown at once, confirmed by the server, rolled back if refused. */
  async setState(scope: Scope, owner: string, key: string, value: unknown): Promise<boolean> {
    const rollback = this.store.setOptimistic(scope, owner, key, value);
    const res = await this.request('POST', '/state', {
      patches: [{ op: 'set', scope, owner, key, value }],
    });
    if (res.ok) {
      this.store.apply((res.json.patches ?? []) as WirePatch[]);
      return true;
    }
    rollback();
    return false;
  }

  // ---- events -------------------------------------------------------------------------------

  /** Sends an event to the plugin's server-side handler and applies the state changes it made. */
  async emit(type: string, target: EmitTarget, payload: Record<string, unknown> = {}): Promise<EventOutcome> {
    const eventId = newId(); // reused across retries so the server never runs the event twice
    const body = {
      eventId,
      type,
      pluginId: target.pluginId,
      componentId: target.componentId,
      instanceId: target.instanceId,
      page: this.page,
      payload,
    };
    const res = await this.request('POST', '/events', body, 2);
    if (res.json?.patches) this.store.apply(res.json.patches as WirePatch[]);
    return {
      ok: res.ok && res.json?.status === 'success',
      message: res.json?.message ?? (res.ok ? undefined : 'Request failed'),
      data: res.json?.data,
      errors: res.json?.errors,
    };
  }

  // ---- auth ---------------------------------------------------------------------------------

  get user(): RuntimeUser | null {
    return (this.store.get('SESSION', '$auth', 'user') as RuntimeUser | undefined) ?? null;
  }

  async login(username: string, password: string): Promise<EventOutcome> {
    const res = await this.request('POST', '/auth/login', { username, password });
    if (res.ok) {
      if (res.json.csrf) this.csrf = res.json.csrf;
      this.store.apply((res.json.patches ?? []) as WirePatch[]);
    }
    return { ok: res.ok, message: res.ok ? undefined : res.json?.message ?? 'Sign-in failed' };
  }

  async logout(): Promise<EventOutcome> {
    const res = await this.request('POST', '/auth/logout', {});
    if (res.ok) {
      if (res.json.csrf) this.csrf = res.json.csrf;
      this.store.apply((res.json.patches ?? []) as WirePatch[]);
      // Signed in through the auth server: end its session too, or the next click signs back in.
      const next = res.json.redirect;
      if (typeof next === 'string' && /^https?:\/\//i.test(next)) location.assign(next);
    }
    return { ok: res.ok, message: res.ok ? undefined : res.json?.message };
  }

  /** True when the site lets visitors sign in through the VSD auth server. */
  get ssoEnabled(): boolean {
    return !!this.boot.sso?.enabled;
  }

  /** False when the site only allows single sign-on. */
  get passwordLogin(): boolean {
    return this.boot.sso?.passwordLogin !== false;
  }

  /** Sends the visitor to the auth server; they come back signed in, on the page they were on. */
  startSso(returnTo: string = location.pathname + location.search): void {
    const start = this.boot.sso?.start ?? '/_vsd/auth/sso/start';
    location.assign(`${start}?return=${encodeURIComponent(returnTo)}`);
  }

  // ---- hooks (published for plugins) ---------------------------------------------------------

  useSharedState = <T,>(scope: Scope, owner: string, key: string, initial?: T): [T | undefined, (v: T) => void] => {
    const value = useSyncExternalStore(
      this.store.subscribe,
      () => this.store.get(scope, owner, key),
    ) as T | undefined;
    const set = useCallback((v: T) => void this.setState(scope, owner, key, v), [scope, owner, key]);
    return [value === undefined ? initial : value, set];
  };

  useAuth = (): {
    user: RuntimeUser | null;
    login: Runtime['login'];
    logout: Runtime['logout'];
    ssoEnabled: boolean;
    passwordLogin: boolean;
    startSso: Runtime['startSso'];
  } => {
    const user = useSyncExternalStore(
      this.store.subscribe,
      () => this.store.get('SESSION', '$auth', 'user'),
    ) as RuntimeUser | undefined;
    return {
      user: user ?? null,
      login: this.login.bind(this),
      logout: this.logout.bind(this),
      ssoEnabled: this.ssoEnabled,
      passwordLogin: this.passwordLogin,
      startSso: this.startSso.bind(this),
    };
  };

  // ---- page data ----------------------------------------------------------------------------

  /** The current page as this visitor should see it now (data and bindings resolved on the server). */
  async fetchPage(): Promise<ComponentInstance[] | null> {
    try {
      const url = `${this.boot.api}/page?path=${encodeURIComponent(location.pathname)}&query=${encodeURIComponent(location.search)}`;
      const res = await fetch(url, { credentials: 'same-origin', cache: 'no-store' });
      if (!res.ok) return null;
      const body = (await res.json()) as { definition?: { components?: ComponentInstance[] } };
      return body.definition?.components ?? null;
    } catch {
      return null;
    }
  }

  // ---- transport ----------------------------------------------------------------------------

  private async request(
    method: 'GET' | 'POST',
    path: string,
    body?: unknown,
    networkRetries = 0,
    allowRefresh = true,
  ): Promise<{ ok: boolean; status: number; json: any }> {
    let res: Response;
    try {
      res = await fetch(this.boot.api + path, {
        method,
        credentials: 'same-origin',
        headers: { 'Content-Type': 'application/json', 'X-VSD-CSRF': this.csrf },
        body: body === undefined ? undefined : JSON.stringify(body),
      });
    } catch {
      if (networkRetries > 0) {
        await new Promise((r) => setTimeout(r, 400));
        return this.request(method, path, body, networkRetries - 1, allowRefresh);
      }
      return { ok: false, status: 0, json: { message: 'Network error' } };
    }
    const json = await res.json().catch(() => ({}));
    if (allowRefresh && (json?.message === 'no_session' || json?.message === 'csrf')) {
      // Session expired, or the CSRF token was rotated by a sign-in in another tab:
      // resync once and retry. (A plain 401 such as "Invalid credentials" is not retried.)
      await this.refreshSession();
      return this.request(method, path, body, networkRetries, false);
    }
    return { ok: res.ok, status: res.status, json };
  }

  private refreshSession(): Promise<void> {
    this.refreshing ??= (async () => {
      try {
        const res = await fetch(this.boot.api + '/session', { credentials: 'same-origin' });
        if (res.ok) {
          const s = await res.json();
          this.csrf = s.csrf;
          this.store.replace(s.entries as WirePatch[], s.rev);
          this.connect();
        }
      } finally {
        this.refreshing = null;
      }
    })();
    return this.refreshing;
  }

  /** Opens (or reopens) the server-sent event stream that keeps every tab in step. */
  connect(): void {
    this.stream?.close();
    const es = new EventSource(`${this.boot.api}/stream?since=${this.store.rev}`);
    this.stream = es;
    es.addEventListener('patch', (e) => {
      this.reconnectDelay = 1000;
      this.store.apply(JSON.parse((e as MessageEvent).data).patches as WirePatch[]);
    });
    es.addEventListener('snapshot', (e) => {
      this.reconnectDelay = 1000;
      const d = JSON.parse((e as MessageEvent).data);
      this.store.replace(d.entries as WirePatch[], d.rev);
    });
    es.addEventListener('ready', () => {
      this.reconnectDelay = 1000;
    });
    es.onerror = () => {
      // The browser retries by itself unless the server refused us (for example 401 after the
      // session expired); in that case start over with a fresh session.
      if (es.readyState === EventSource.CLOSED && this.stream === es) {
        const delay = this.reconnectDelay;
        this.reconnectDelay = Math.min(delay * 2, 30_000);
        setTimeout(() => void this.refreshSession().then(() => this.stream === es && this.connect()), delay);
      }
    };
  }
}

declare global {
  interface Window {
    VSD?: { runtime: Runtime; version: string };
  }
}
