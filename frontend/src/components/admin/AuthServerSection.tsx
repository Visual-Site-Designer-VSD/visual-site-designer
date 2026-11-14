import React, { useEffect, useState } from 'react';
import { ModalButton, ModalField, ModalSection } from '../modals/ModalBase';
import {
  AuthServerCheck,
  AuthServerSettings,
  AuthServerUpdate,
  authServerError,
  authServerService,
} from '../../services/authServerService';
import './AuthServerSection.css';

const empty: AuthServerUpdate = {
  enabled: false,
  issuerUri: '',
  authorizationUri: '',
  tokenUri: '',
  jwkSetUri: '',
  userInfoUri: '',
  clientId: '',
  clientSecret: '',
  scopes: 'openid profile email',
};

const fromSettings = (s: AuthServerSettings): AuthServerUpdate => ({
  enabled: s.enabled,
  issuerUri: s.issuerUri ?? '',
  authorizationUri: s.authorizationUri ?? '',
  tokenUri: s.tokenUri ?? '',
  jwkSetUri: s.jwkSetUri ?? '',
  userInfoUri: s.userInfoUri ?? '',
  clientId: s.clientId ?? '',
  clientSecret: '', // the saved secret is never sent to the browser
  scopes: s.scopes,
});

const trimSlashes = (u: string) => u.trim().replace(/\/+$/, '');

/**
 * Admin section: how people sign in through the VSD Auth Server. Changes apply to the next sign-in, with no
 * restart. The auth server needs no discovery document: give the issuer address and the usual endpoints are
 * filled in, or type each address.
 */
export const AuthServerSection: React.FC = () => {
  const [settings, setSettings] = useState<AuthServerSettings | null>(null);
  const [form, setForm] = useState<AuthServerUpdate>(empty);
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState<'save' | 'test' | 'reset' | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [message, setMessage] = useState<string | null>(null);
  const [checks, setChecks] = useState<AuthServerCheck[] | null>(null);
  const [copied, setCopied] = useState(false);

  useEffect(() => {
    authServerService
      .get()
      .then((s) => {
        setSettings(s);
        setForm(fromSettings(s));
      })
      .catch((e) => setError(authServerError(e)))
      .finally(() => setLoading(false));
  }, []);

  const set = <K extends keyof AuthServerUpdate>(key: K, value: AuthServerUpdate[K]) => {
    setForm((f) => ({ ...f, [key]: value }));
    setMessage(null);
    setChecks(null);
  };

  /** Fills the endpoints from the issuer address with the usual paths. */
  const fillFromIssuer = () => {
    const base = trimSlashes(form.issuerUri);
    if (!base) {
      setError('Enter the issuer address first.');
      return;
    }
    setError(null);
    setForm((f) => ({
      ...f,
      authorizationUri: `${base}/oauth2/authorize`,
      tokenUri: `${base}/oauth2/token`,
      jwkSetUri: `${base}/oauth2/jwks`,
      userInfoUri: `${base}/userinfo`,
    }));
    setChecks(null);
  };

  const run = async (kind: 'save' | 'test' | 'reset', action: () => Promise<void>) => {
    setBusy(kind);
    setError(null);
    setMessage(null);
    try {
      await action();
    } catch (e) {
      setError(authServerError(e));
    } finally {
      setBusy(null);
    }
  };

  const save = () =>
    run('save', async () => {
      const s = await authServerService.save(form);
      setSettings(s);
      setForm(fromSettings(s));
      setChecks(null);
      setMessage(
        s.enabled
          ? 'Saved. People can sign in with the VSD Auth Server from now on.'
          : 'Saved. Sign-in with the VSD Auth Server is turned off.',
      );
    });

  const test = () =>
    run('test', async () => {
      const r = await authServerService.test(form);
      setChecks(r.checks);
    });

  const reset = () =>
    run('reset', async () => {
      if (!window.confirm('Forget the saved settings and go back to the application configuration?')) return;
      const s = await authServerService.reset();
      setSettings(s);
      setForm(fromSettings(s));
      setChecks(null);
      setMessage('Back to the application configuration.');
    });

  const copyRedirect = async () => {
    if (!settings) return;
    try {
      await navigator.clipboard.writeText(settings.redirectUri);
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    } catch {
      /* the address is visible, so it can be copied by hand */
    }
  };

  if (loading) return <ModalSection title="Authorization server"><div className="auth-server-note">Loading…</div></ModalSection>;

  return (
    <ModalSection title="Authorization server (VSD Auth Server)">
      <p className="auth-server-note">
        Let people sign in to this CMS with the VSD Auth Server. Changes apply to the next sign-in; no restart is needed.
        {settings?.source === 'environment' && ' These values still come from the application configuration until you save.'}
      </p>

      {error && <div className="auth-server-error" role="alert">{error}</div>}
      {message && <div className="auth-server-success" role="status">{message}</div>}

      <label className="auth-server-toggle">
        <input type="checkbox" checked={form.enabled} onChange={(e) => set('enabled', e.target.checked)} />
        <span>Allow sign-in with the VSD Auth Server</span>
      </label>

      <ModalField label="Issuer address" htmlFor="as-issuer" hint="For example https://auth.example.com. Only used to fill in the endpoints below and to check ID tokens.">
        <input id="as-issuer" type="text" value={form.issuerUri} onChange={(e) => set('issuerUri', e.target.value)} placeholder="https://auth.example.com" />
      </ModalField>
      <div className="auth-server-inline">
        <ModalButton variant="secondary" onClick={fillFromIssuer} type="button">Fill in the usual endpoints</ModalButton>
      </div>

      <ModalField label="Authorization endpoint" htmlFor="as-authz">
        <input id="as-authz" type="text" value={form.authorizationUri} onChange={(e) => set('authorizationUri', e.target.value)} placeholder="https://auth.example.com/oauth2/authorize" />
      </ModalField>
      <ModalField label="Token endpoint" htmlFor="as-token">
        <input id="as-token" type="text" value={form.tokenUri} onChange={(e) => set('tokenUri', e.target.value)} placeholder="https://auth.example.com/oauth2/token" />
      </ModalField>
      <ModalField label="Signing keys (JWKS) address" htmlFor="as-jwks">
        <input id="as-jwks" type="text" value={form.jwkSetUri} onChange={(e) => set('jwkSetUri', e.target.value)} placeholder="https://auth.example.com/oauth2/jwks" />
      </ModalField>
      <ModalField label="User info endpoint (optional)" htmlFor="as-userinfo">
        <input id="as-userinfo" type="text" value={form.userInfoUri} onChange={(e) => set('userInfoUri', e.target.value)} placeholder="https://auth.example.com/userinfo" />
      </ModalField>

      <ModalField label="Client ID" htmlFor="as-client">
        <input id="as-client" type="text" value={form.clientId} onChange={(e) => set('clientId', e.target.value)} autoComplete="off" />
      </ModalField>
      <ModalField
        label="Client secret"
        htmlFor="as-secret"
        hint={settings?.clientSecretSet ? 'A secret is saved. Leave this empty to keep it.' : 'Not set yet.'}
      >
        <input
          id="as-secret"
          type="password"
          value={form.clientSecret}
          onChange={(e) => set('clientSecret', e.target.value)}
          placeholder={settings?.clientSecretSet ? '••••••••  (saved)' : ''}
          autoComplete="new-password"
        />
      </ModalField>
      <ModalField label="Scopes" htmlFor="as-scopes">
        <input id="as-scopes" type="text" value={form.scopes} onChange={(e) => set('scopes', e.target.value)} />
      </ModalField>

      {settings && (
        <ModalField label="Redirect address to register at the auth server" htmlFor="as-redirect" hint="Add this exact address to the client's allowed redirect addresses.">
          <div className="auth-server-copy">
            <input id="as-redirect" type="text" readOnly value={settings.redirectUri} />
            <ModalButton variant="secondary" onClick={copyRedirect} type="button">{copied ? 'Copied' : 'Copy'}</ModalButton>
          </div>
        </ModalField>
      )}

      {checks && (
        <ul className="auth-server-checks" aria-label="Connection test">
          {checks.map((c) => (
            <li key={c.name} className={c.ok ? 'ok' : 'bad'}>
              <span aria-hidden="true">{c.ok ? '✓' : '✗'}</span> <strong>{c.name}</strong>: {c.message}
            </li>
          ))}
        </ul>
      )}

      <div className="auth-server-actions">
        <ModalButton variant="secondary" onClick={test} loading={busy === 'test'} disabled={busy !== null} type="button">
          Test connection
        </ModalButton>
        <ModalButton variant="primary" onClick={save} loading={busy === 'save'} disabled={busy !== null} type="button">
          Save
        </ModalButton>
        {settings?.source === 'database' && (
          <ModalButton variant="secondary" onClick={reset} loading={busy === 'reset'} disabled={busy !== null} type="button">
            Use application configuration
          </ModalButton>
        )}
      </div>
    </ModalSection>
  );
};

export default AuthServerSection;
