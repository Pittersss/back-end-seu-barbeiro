import { useMemo } from 'react';
import { StyleSheet } from 'react-native';

import type { ColorTokens } from './colors';
import { useThemeColors } from './ThemeContext';

/**
 * `StyleSheet.create` only ever runs once per module on native (its return value is a
 * cached style-id lookup), so a plain module-level stylesheet built from `colors.*`
 * would freeze in whatever scheme was active on first import. This rebuilds the
 * stylesheet whenever the active theme's colors change, memoized so it's cheap on the
 * (far more common) renders where the scheme hasn't changed.
 */
export function useThemedStyles<T extends StyleSheet.NamedStyles<T> | StyleSheet.NamedStyles<unknown>>(
  factory: (colors: ColorTokens) => T,
): T {
  const colors = useThemeColors();
  return useMemo(() => StyleSheet.create(factory(colors)), [colors]);
}
