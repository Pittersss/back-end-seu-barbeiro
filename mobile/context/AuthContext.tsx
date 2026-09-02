import { createContext, useContext, useEffect, useMemo, useState, type ReactNode } from 'react';

import * as authApi from '../lib/api/auth';
import { getMe } from '../lib/api/users';
import { setAuthToken, setUnauthorizedHandler } from '../lib/api';
import * as storage from '../lib/storage';
import type {
  AuthResponse,
  LoginRequest,
  RegisterBarberRequest,
  RegisterClientRequest,
  ResendCodeRequest,
  UserRole,
  VerifyEmailRequest,
} from '../lib/types';

const STORAGE_KEY = 'seu-barbeiro-session';

interface Session {
  token: string;
  userId: number;
  role: UserRole;
  name: string;
  avatarBase64?: string | null;
}

interface AuthContextValue {
  session: Session | null;
  isLoading: boolean;
  login: (payload: LoginRequest) => Promise<Session>;
  registerClient: (payload: RegisterClientRequest) => Promise<void>;
  registerBarber: (payload: RegisterBarberRequest) => Promise<void>;
  verifyEmail: (payload: VerifyEmailRequest) => Promise<Session>;
  resendCode: (payload: ResendCodeRequest) => Promise<void>;
  /** Re-pull /api/users/me and fold name + avatar into the persisted session. */
  refreshProfile: () => Promise<void>;
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

  async function clearSession() {
    setAuthToken(null);
    setSession(null);
    await storage.deleteItem(STORAGE_KEY);
  }

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

    // A 401 means the token is missing/invalid/stale (e.g. the account it
    // belonged to was deleted) — drop the session instead of letting every
    // screen's data fetch fail with an unhandled error.
    setUnauthorizedHandler(() => {
      clearSession();
    });
    return () => setUnauthorizedHandler(null);
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
        await authApi.registerClient(payload);
      },
      registerBarber: async (payload) => {
        await authApi.registerBarber(payload);
      },
      verifyEmail: async (payload) => {
        const next = toSession(await authApi.verifyEmail(payload));
        await persist(next);
        return next;
      },
      resendCode: async (payload) => {
        await authApi.resendCode(payload);
      },
      refreshProfile: async () => {
        if (!session) return;
        const profile = await getMe();
        await persist({
          ...session,
          name: profile.name,
          avatarBase64: profile.avatarBase64 ?? null,
        });
      },
      logout: clearSession,
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
