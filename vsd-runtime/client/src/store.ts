import type { Scope, WirePatch } from './types';

export const keyOf = (scope: Scope, owner: string, key: string): string => `${scope}|${owner}|${key}`;

interface Entry {
  value: unknown;
  rev: number;
}

/**
 * Client mirror of the session state held by the server. Every change carries a revision and
 * is applied only if it is newer than what we hold for that key, so patches can arrive twice
 * (HTTP response and SSE) or out of order without corrupting state. Removed keys keep a
 * tombstone revision so a late, older "set" cannot bring them back.
 */
export class StateStore {
  private entries = new Map<string, Entry>();
  private tombstones = new Map<string, number>();
  private listeners = new Set<() => void>();
  private _rev = 0;

  get rev(): number {
    return this._rev;
  }

  get(scope: Scope, owner: string, key: string): unknown {
    return this.entries.get(keyOf(scope, owner, key))?.value;
  }

  subscribe = (listener: () => void): (() => void) => {
    this.listeners.add(listener);
    return () => this.listeners.delete(listener);
  };

  /** Applies server patches; returns true if anything changed. */
  apply(patches: WirePatch[]): boolean {
    let changed = false;
    for (const p of patches) {
      const k = keyOf(p.scope, p.owner, p.key);
      const known = this.entries.get(k)?.rev ?? this.tombstones.get(k) ?? 0;
      if (p.rev <= known) continue;
      if (p.op === 'set') {
        this.entries.set(k, { value: p.value, rev: p.rev });
        this.tombstones.delete(k);
      } else {
        this.entries.delete(k);
        this.tombstones.set(k, p.rev);
      }
      this._rev = Math.max(this._rev, p.rev);
      changed = true;
    }
    if (changed) this.emit();
    return changed;
  }

  /** Replaces everything with a server snapshot (first load, or after the server lost our place). */
  replace(entries: WirePatch[], rev: number): void {
    this.entries.clear();
    this.tombstones.clear();
    for (const e of entries) {
      this.entries.set(keyOf(e.scope, e.owner, e.key), { value: e.value, rev: e.rev });
    }
    this._rev = rev;
    this.emit();
  }

  /** Shows a local change at once; the server patch that follows has a higher rev and replaces it. */
  setOptimistic(scope: Scope, owner: string, key: string, value: unknown): () => void {
    const k = keyOf(scope, owner, key);
    const before = this.entries.get(k);
    const baseRev = before?.rev ?? this.tombstones.get(k) ?? 0;
    this.entries.set(k, { value, rev: baseRev });
    this.emit();
    return () => {
      // Roll back only if nothing newer arrived meanwhile.
      const now = this.entries.get(k);
      if (now && now.value === value && now.rev === baseRev) {
        if (before) this.entries.set(k, before);
        else this.entries.delete(k);
        this.emit();
      }
    };
  }

  private emit(): void {
    this.listeners.forEach((l) => l());
  }
}
