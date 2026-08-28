import { createContext, useContext, useEffect, useMemo, useState, type ReactNode } from 'react';

import * as authApi from '../lib/api/auth';
import { setAuthToken } from '../lib/api';
import * as storage from '../lib/storage';
import type { AuthResponse, LoginRequest, RegisterBarberRequest, RegisterClientRequest, UserRole } from '../lib/types';

const STORAGE_KEY = 'seu-barbeiro-session';

interface Session {
  token: string;
  userId: number;
  role: UserRole;
  name: string;
}

interface AuthContextValue {
  session: Session | null;
  isLoading: boolean;
  login: (payload: LoginRequest) => Promise<Session>;
  registerClient: (payload: RegisterClientRequest) => Promise<Session>;
  registerBarber: (payload: RegisterBarberRequest) => Promise<Session>;
  logout: () => Promise<void>;
}

const AuthContext = createContext<AuthContextValue | undefined>(undefined);

function toSession(response: AuthResponse): Session {
  return {
    token: response.token,
    userId: response.userId,
    role: response.role,
    name: response.name,
  };
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [session, setSession] = useState<Session | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    (async () => {
      const stored = await storage.getItem(STORAGE_KEY);
      if (stored) {
        const parsed: Session = JSON.parse(stored);
        setAuthToken(parsed.token);
        setSession(parsed);
      }
      setIsLoading(false);
    })();
  }, []);

  async function persist(next: Session) {
    setAuthToken(next.token);
    setSession(next);
    await storage.setItem(STORAGE_KEY, JSON.stringify(next));
  }

  const value = useMemo<AuthContextValue>(
    () => ({
      session,
      isLoading,
      login: async (payload) => {
        const next = toSession(await authApi.login(payload));
        await persist(next);
        return next;
      },
      registerClient: async (payload) => {
        const next = toSession(await authApi.registerClient(payload));
        await persist(next);
        return next;
      },
      registerBarber: async (payload) => {
        const next = toSession(await authApi.registerBarber(payload));
        await persist(next);
        return next;
      },
      logout: async () => {
        setAuthToken(null);
        setSession(null);
        await storage.deleteItem(STORAGE_KEY);
      },
    }),
    [session, isLoading],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return ctx;
}
