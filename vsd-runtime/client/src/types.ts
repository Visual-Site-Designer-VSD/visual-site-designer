export type Scope = 'SESSION' | 'PAGE' | 'PLUGIN' | 'COMPONENT';

/** Wire form of one state change or entry. */
export interface WirePatch {
  op: 'set' | 'remove';
  scope: Scope;
  owner: string;
  key: string;
  value?: unknown;
  rev: number;
}

export interface RuntimeUser {
  id: string;
  username: string;
  displayName: string;
  email?: string;
  roles: string[];
  avatarUrl?: string;
  provider?: 'password' | 'sso';
}

/** JSON embedded in the page shell by the server. */
export interface Boot {
  site: { name: string; title: string; version: string };
  page: { name: string; path: string; title: string; definition: PageDefinition };
  pages: { name: string; path: string; title: string }[];
  plugins: { id: string; version: string; css: boolean }[];
  session: { csrf: string; rev: number; entries: WirePatch[] };
  api: string;
  sso: { enabled: boolean; passwordLogin: boolean; start: string };
  shared: SharedDef[];
}

/** A shared state declared in the runtime console. The browser falls back to `default` until a value exists. */
export interface SharedDef {
  name: string;
  kind: 'OBJECT' | 'MAP';
  lifetime: 'VISITOR' | 'LOGIN';
  default: unknown;
  clientWritable: boolean;
  writers: string[];
}

export interface ComponentInstance {
  instanceId: string;
  pluginId: string;
  componentId: string;
  props: Record<string, unknown>;
  styles: Record<string, string>;
  children?: ComponentInstance[];
  displayOrder?: number;
  isVisible?: boolean;
  [k: string]: unknown;
}

export interface PageDefinition {
  pageName: string;
  components: ComponentInstance[];
  [k: string]: unknown;
}

export interface EventOutcome {
  ok: boolean;
  message?: string;
  data?: Record<string, unknown>;
  errors?: Record<string, string>;
}
