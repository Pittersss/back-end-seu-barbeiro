import { createContext, useContext, useEffect, useMemo, useState, type ReactNode } from 'react';

import * as storage from '../lib/storage';
import { darkColors, lightColors, type ColorScheme, type ColorTokens } from './colors';

const STORAGE_KEY = 'seu-barbeiro-theme';

interface ThemeContextValue {
  scheme: ColorScheme;
  colors: ColorTokens;
  isDark: boolean;
  toggleScheme: () => void;
  setScheme: (scheme: ColorScheme) => void;
}

const ThemeContext = createContext<ThemeContextValue | undefined>(undefined);

export function ThemeProvider({ children }: { children: ReactNode }) {
  // Design direction is light-by-default (see AGENTS.md) — dark mode is an opt-in the
  // user turns on themselves, not something derived from the OS scheme.
  const [scheme, setSchemeState] = useState<ColorScheme>('light');

  useEffect(() => {
    let cancelled = false;
    (async () => {
      const stored = await storage.getItem(STORAGE_KEY);
      if (!cancelled && (stored === 'dark' || stored === 'light')) {
        setSchemeState(stored);
      }
    })();
    return () => {
      cancelled = true;
    };
  }, []);

  const setScheme = (next: ColorScheme) => {
    setSchemeState(next);
    storage.setItem(STORAGE_KEY, next).catch(() => {});
  };

  const toggleScheme = () => setScheme(scheme === 'dark' ? 'light' : 'dark');

  const value = useMemo<ThemeContextValue>(
    () => ({
      scheme,
      colors: scheme === 'dark' ? darkColors : lightColors,
      isDark: scheme === 'dark',
      toggleScheme,
      setScheme,
    }),
    [scheme],
  );

  return <ThemeContext.Provider value={value}>{children}</ThemeContext.Provider>;
}

export function useTheme(): ThemeContextValue {
  const ctx = useContext(ThemeContext);
  if (!ctx) {
    throw new Error('useTheme must be used within a ThemeProvider');
  }
  return ctx;
}

export function useThemeColors(): ColorTokens {
  return useTheme().colors;
}
