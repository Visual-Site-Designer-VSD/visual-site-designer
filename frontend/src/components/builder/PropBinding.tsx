import React, { useEffect, useMemo, useState } from 'react';
import { useRuntimeStore } from '../../stores/runtimeStore';
import { BindingOption, appendBinding, bindingOptions, undeclaredReads, wrapPath } from '../../utils/bindings';
import './PropBinding.css';

interface PropBindingProps {
  /** The current template for this prop, if it is bound. */
  value?: string;
  /** Called with the new template, or undefined to remove the binding. */
  onChange: (next: string | undefined) => void;
}

const isWebUrl = (u: string | undefined): u is string => !!u && /^https?:\/\//i.test(u);

/**
 * Lets a designer fill a prop from live data: shared state declared in the runtime console, the signed-in
 * user, the page or the URL. The binding is stored as a template ("Total {{shared.cart.total}}") and the live
 * runtime fills it in for each visitor.
 */
export const PropBinding: React.FC<PropBindingProps> = ({ value, onChange }) => {
  const { catalog, error, load } = useRuntimeStore();
  const [open, setOpen] = useState(false);
  const [pending, setPending] = useState<{ option: BindingOption; key: string } | null>(null);
  const bound = value !== undefined && value !== '';
  const editing = open || bound;

  useEffect(() => {
    if (editing) void load();
  }, [editing, load]);

  const declared = useMemo(() => (catalog?.connected ? catalog.definitions : []), [catalog]);
  const options = useMemo(() => bindingOptions(declared), [declared]);
  const groups = useMemo(() => {
    const map = new Map<string, BindingOption[]>();
    options.forEach((o) => map.set(o.group, [...(map.get(o.group) ?? []), o]));
    return [...map.entries()];
  }, [options]);

  if (!editing) {
    return (
      <button type="button" className="prop-binding-link" onClick={() => setOpen(true)}>
        🔗 Bind to live data
      </button>
    );
  }

  const unknown = catalog?.connected ? undeclaredReads(value, declared) : [];
  const insert = (path: string) => onChange(appendBinding(value ?? '', path));

  const choose = (index: string) => {
    if (index === '') return;
    const option = options[Number(index)];
    if (option.needsKey) setPending({ option, key: '' });
    else insert(option.path);
  };

  const confirmKey = () => {
    if (!pending || !/^[A-Za-z0-9_-]{1,64}$/.test(pending.key)) return;
    insert(pending.option.path + pending.key);
    setPending(null);
  };

  return (
    <div className="prop-binding-box">
      <div className="prop-binding-head">
        <span>🔗 Live binding</span>
        <button
          type="button"
          className="prop-binding-remove"
          onClick={() => {
            onChange(undefined);
            setOpen(false);
            setPending(null);
          }}
        >
          Remove
        </button>
      </div>

      <input
        type="text"
        className="prop-binding-input"
        value={value ?? ''}
        placeholder={`Hello ${wrapPath('auth.user.displayName')}`}
        onChange={(e) => onChange(e.target.value === '' ? undefined : e.target.value)}
        spellCheck={false}
      />

      <select className="prop-binding-select" value="" onChange={(e) => choose(e.target.value)}>
        <option value="">Insert a value…</option>
        {groups.map(([group, items]) => (
          <optgroup key={group} label={group}>
            {items.map((o) => (
              <option key={o.path} value={options.indexOf(o)}>
                {o.label}
                {o.hint ? ` (${o.hint})` : ''}
              </option>
            ))}
          </optgroup>
        ))}
      </select>

      {pending && (
        <div className="prop-binding-key">
          <label>
            {pending.option.path.startsWith('query.') ? 'URL parameter name' : 'Key'}
            <input
              type="text"
              value={pending.key}
              autoFocus
              onChange={(e) => setPending({ ...pending, key: e.target.value })}
              onKeyDown={(e) => {
                if (e.key === 'Enter') {
                  e.preventDefault();
                  confirmKey();
                }
              }}
            />
          </label>
          <button type="button" onClick={confirmKey}>Insert</button>
          <button type="button" onClick={() => setPending(null)}>Cancel</button>
        </div>
      )}

      {unknown.map((name) => (
        <p key={name} className="prop-binding-warning" role="alert">
          ⚠ Shared state “{name}” is not declared in the runtime console. Publishing will be refused until it is.
        </p>
      ))}

      {error && <p className="prop-binding-note">Could not check shared state names right now.</p>}
      {!error && catalog && !catalog.configured && (
        <p className="prop-binding-note">
          The builder is not linked to a runtime, so declared shared state cannot be offered or checked.
          Set <code>vsd.runtime-link.url</code> on the server.
        </p>
      )}
      {!error && catalog?.configured && !catalog.connected && (
        <p className="prop-binding-note">{catalog.message ?? 'The runtime did not answer.'} Names cannot be checked.</p>
      )}
      {!error && catalog?.connected && catalog.definitions.length === 0 && (
        <p className="prop-binding-note">
          No shared state is declared yet.
          {catalog.consoleUrl
            ? ' Declare it in the runtime console first.'
            : ' Choose Preview > Run on live server, then Preview > Open runtime console to declare it.'}
        </p>
      )}
      {catalog?.configured && isWebUrl(catalog.consoleUrl) && (
        <p className="prop-binding-note">
          <a href={catalog.consoleUrl} target="_blank" rel="noopener noreferrer">Open the runtime console</a>
        </p>
      )}

      <small className="prop-binding-help">
        The live runtime fills this in for each visitor. The builder canvas still shows the prop’s normal value.
      </small>
    </div>
  );
};

export default PropBinding;
