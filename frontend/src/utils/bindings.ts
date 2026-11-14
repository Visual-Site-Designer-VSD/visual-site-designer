import type { SharedStateDefinition } from '../services/runtimeService';

/**
 * Helpers for template bindings such as "Total: {{shared.cart.total}}". The live runtime fills these in per
 * visitor; the builder only writes them and checks the names.
 */

const SHARED_REF = /\{\{\s*shared\.([A-Za-z0-9_-]+)/g;

export interface BindingOption {
  /** Group heading in the picker. */
  group: string;
  /** What the designer sees. */
  label: string;
  /** The path inside the braces, for example `shared.cart.total`. For `needsKey` options it ends with a dot. */
  path: string;
  hint?: string;
  /** The designer still has to type the key (a map entry or a URL parameter). */
  needsKey?: boolean;
}

export const wrapPath = (path: string): string => `{{${path}}}`;

/** Names of the shared state a template reads. */
export function sharedReads(template: string | undefined): string[] {
  if (!template) return [];
  const names = new Set<string>();
  for (const m of template.matchAll(SHARED_REF)) names.add(m[1]);
  return [...names];
}

/** Names the template reads that are not in the declared set. */
export function undeclaredReads(template: string | undefined, declared: SharedStateDefinition[]): string[] {
  const known = new Set(declared.map((d) => d.name));
  return sharedReads(template).filter((n) => !known.has(n));
}

const AUTH_OPTIONS: BindingOption[] = [
  { group: 'Signed-in user', label: 'Is signed in', path: 'auth.isAuthenticated', hint: 'true / false' },
  { group: 'Signed-in user', label: 'Display name', path: 'auth.user.displayName' },
  { group: 'Signed-in user', label: 'Username', path: 'auth.user.username' },
  { group: 'Signed-in user', label: 'Email', path: 'auth.user.email' },
  { group: 'Signed-in user', label: 'User id', path: 'auth.user.id' },
  { group: 'Signed-in user', label: 'Roles', path: 'auth.user.roles', hint: 'list' },
  { group: 'Signed-in user', label: 'Avatar URL', path: 'auth.user.avatarUrl' },
];

const PAGE_OPTIONS: BindingOption[] = [
  { group: 'Page', label: 'Page title', path: 'page.title' },
  { group: 'Page', label: 'Page name', path: 'page.name' },
  { group: 'Page', label: 'Page path', path: 'page.path' },
  { group: 'URL', label: 'URL parameter…', path: 'query.', hint: 'type the parameter name', needsKey: true },
];

/** Everything a designer can bind a prop to, with the declared shared state first. */
export function bindingOptions(declared: SharedStateDefinition[]): BindingOption[] {
  const out: BindingOption[] = [];
  for (const d of declared) {
    const group = `Shared state: ${d.name}`;
    const hint = `${d.kind === 'MAP' ? 'map' : 'object'}, ${d.lifetime === 'LOGIN' ? 'while signed in' : 'per visitor'}`;
    if (d.kind === 'OBJECT') {
      for (const [field, f] of Object.entries(d.fields ?? {})) {
        out.push({ group, label: `${d.name}.${field}`, path: `shared.${d.name}.${field}`, hint: f.type });
      }
    } else {
      out.push({ group, label: `${d.name}.<key>…`, path: `shared.${d.name}.`, hint: `map of ${d.valueType ?? 'any'}`, needsKey: true });
    }
    out.push({ group, label: `${d.name} (whole value)`, path: `shared.${d.name}`, hint });
  }
  return [...out, ...AUTH_OPTIONS, ...PAGE_OPTIONS];
}

/** Adds a binding to what is already typed, with a space between pieces of text. */
export function appendBinding(current: string, path: string): string {
  const piece = wrapPath(path);
  return current.trim() === '' ? piece : `${current}${/\s$/.test(current) ? '' : ' '}${piece}`;
}
