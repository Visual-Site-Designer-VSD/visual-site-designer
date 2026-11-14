import React, { useEffect } from 'react';
import type { ComponentManifest } from '../../types/builder';
import { useRuntimeStore } from '../../stores/runtimeStore';
import './PropBinding.css';

/**
 * What the selected component says it needs from shared state (declared by the plugin in its manifest),
 * next to what the runtime console has actually declared.
 */
export const SharedStateUsage: React.FC<{ manifest: ComponentManifest | null }> = ({ manifest }) => {
  const { catalog, load } = useRuntimeStore();
  const reads = manifest?.sharedState?.reads ?? [];
  const writes = manifest?.sharedState?.writes ?? [];
  const wanted = reads.length + writes.length > 0;

  useEffect(() => {
    if (wanted) void load();
  }, [wanted, load]);

  if (!wanted) return null;
  const known = catalog?.connected ? new Set(catalog.definitions.map((d) => d.name)) : null;

  const row = (name: string, role: string) => {
    const status = known === null ? '' : known.has(name) ? 'ok' : 'missing';
    return (
      <li key={`${role}:${name}`} className={`shared-usage-row ${status}`}>
        <span className="shared-usage-icon" aria-hidden="true">{status === 'ok' ? '✓' : status === 'missing' ? '⚠' : '•'}</span>
        <code>{name}</code>
        <span className="shared-usage-role">{role}</span>
        {status === 'missing' && <span className="shared-usage-note">not declared in the runtime console</span>}
      </li>
    );
  };

  return (
    <div className="shared-usage">
      <div className="shared-usage-title">Shared state this component uses</div>
      <ul>
        {reads.map((n) => row(n, 'reads'))}
        {writes.map((n) => row(n, 'writes'))}
      </ul>
      {known === null && <small className="prop-binding-help">Runtime not connected, so these cannot be checked.</small>}
    </div>
  );
};

export default SharedStateUsage;
