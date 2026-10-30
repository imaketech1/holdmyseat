import { useState, type FormEvent } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { ApiError } from '../api';
import { useAuth } from '../auth';

export default function LoginPage() {
  const { login, register } = useAuth();
  const navigate = useNavigate();
  const from = (useLocation().state as { from?: string } | null)?.from ?? '/';
  const [mode, setMode] = useState<'signin' | 'register'>('signin');
  const [fullName, setFullName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);

  async function submit(e: FormEvent) {
    e.preventDefault();
    setError('');
    setBusy(true);
    try {
      if (mode === 'signin') await login(email, password);
      else await register(fullName, email, password);
      navigate(from, { replace: true });
    } catch (err) {
      const status = err instanceof ApiError ? err.status : 0;
      if (mode === 'signin' && status === 401) setError('That email and password don’t match an account.');
      else if (mode === 'register' && status === 409) setError('An account with this email already exists. Sign in instead.');
      else if (mode === 'register' && status === 400) setError('Use a valid email and a password of at least 8 characters.');
      else setError(err instanceof Error ? err.message : 'Could not reach the server.');
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="login">
      <section className="login-pitch" aria-label="About HoldMySeat">
        <h1>Pick a seat. It’s yours for seven&nbsp;minutes.</h1>
        <p>
          When you choose seats they’re held for you alone while you pay. Everyone else watching the same
          show sees them turn amber in real time, so no two people ever end up with the same seat.
        </p>
      </section>

      <form className="login-form" onSubmit={submit}>
        <div className="tabs" role="tablist" aria-label="Account">
          <button type="button" role="tab" aria-selected={mode === 'signin'} onClick={() => setMode('signin')}>Sign in</button>
          <button type="button" role="tab" aria-selected={mode === 'register'} onClick={() => setMode('register')}>Create account</button>
        </div>

        {mode === 'register' && (
          <label>
            Full name
            <input value={fullName} onChange={(e) => setFullName(e.target.value)} required autoComplete="name" />
          </label>
        )}
        <label>
          Email
          <input type="email" value={email} onChange={(e) => setEmail(e.target.value)} required autoComplete="email" />
        </label>
        <label>
          Password
          <input
            type="password" value={password} onChange={(e) => setPassword(e.target.value)} required
            minLength={mode === 'register' ? 8 : undefined}
            autoComplete={mode === 'register' ? 'new-password' : 'current-password'}
          />
        </label>

        {error && <p className="error" role="alert">{error}</p>}

        <button className="primary" disabled={busy}>
          {busy ? 'One moment' : mode === 'signin' ? 'Sign in' : 'Create account'}
        </button>
      </form>
    </div>
  );
}
