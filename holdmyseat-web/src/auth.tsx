import { createContext, useCallback, useContext, useEffect, useState, type ReactNode } from 'react';
import { api, getTokens, setTokens, type Me, type Tokens } from './api';

type AuthState = {
  me: Me | null;
  loading: boolean;
  login: (email: string, password: string) => Promise<void>;
  register: (fullName: string, email: string, password: string) => Promise<void>;
  logout: () => Promise<void>;
};

const AuthContext = createContext<AuthState | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [me, setMe] = useState<Me | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (!getTokens()) {
      setLoading(false);
      return;
    }
    api<Me>('/api/auth/me')
      .then(setMe)
      .catch(() => setTokens(null))
      .finally(() => setLoading(false));
  }, []);

  const login = useCallback(async (email: string, password: string) => {
    setTokens(await api<Tokens>('/api/auth/login', { method: 'POST', body: { email, password } }));
    setMe(await api<Me>('/api/auth/me'));
  }, []);

  const register = useCallback(async (fullName: string, email: string, password: string) => {
    await api('/api/auth/register', { method: 'POST', body: { fullName, email, password } });
    await login(email, password);
  }, [login]);

  const logout = useCallback(async () => {
    try {
      await api('/api/auth/logout', { method: 'POST' });
    } finally {
      setTokens(null);
      setMe(null);
    }
  }, []);

  return <AuthContext.Provider value={{ me, loading, login, register, logout }}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth outside AuthProvider');
  return ctx;
}
