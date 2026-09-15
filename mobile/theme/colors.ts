export type ColorScheme = 'light' | 'dark';

export interface ColorTokens {
  black: string;
  white: string;
  red: string;
  blue: string;
  bg: string;
  surface: string;
  surfaceAlt: string;
  line: string;
  pill: string;
  pillBorder: string;
  textMuted: string;
  textFaint: string;
  success: string;
  warning: string;
  danger: string;
  overlay: string;
  blueSoft: string;
  successSoft: string;
  warningSoft: string;
  dangerSoft: string;
  /**
   * Fixed white for text/icons drawn on a saturated accent fill (a primary button, a
   * badge dot, a FAB) — those fills don't change between light and dark, so neither
   * should their foreground. Unlike `black`/`white` (which act as "ink"/"paper" and
   * flip with the scheme), this token never changes.
   */
  onAccent: string;
}

export const lightColors: ColorTokens = {
  black: '#0A0A0A',
  white: '#FFFFFF',
  red: '#C1272D',
  blue: '#2F37C9',
  // Page canvas — a warm off-white. Cards/inputs stay pure white on top of it,
  // which gives the layout depth without leaning on heavy shadows (minimalist
  // sites use a tinted ground + white surfaces instead of drop shadows).
  bg: '#F6F6F3',
  // Neutral surfaces / hairlines — same warm greyscale family, pulled out so
  // cards and section backgrounds read as one system.
  surface: '#FFFFFF',
  surfaceAlt: '#F1F1EE',
  line: '#E7E7E2',
  pill: '#ECECE8',
  pillBorder: '#DCDCD6',
  textMuted: '#78787D',
  textFaint: '#B4B4B0',
  success: '#2E7D32',
  warning: '#B8860B',
  danger: '#C1272D',
  overlay: 'rgba(10,10,10,0.45)',
  // Soft tints of the base hues — filled badges, selected pills, callout
  // backgrounds. Derived from the hues above, not new brand colors.
  blueSoft: '#ECEDFB',
  successSoft: '#E7F1E8',
  warningSoft: '#F6EEDD',
  dangerSoft: '#F7E7E7',
  onAccent: '#FFFFFF',
};

// Same token set, remapped for a dark canvas. `black` ("ink") and `white` ("paper")
// trade places — every screen that colors text/icons/borders off `colors.black` and
// surfaces off `colors.white` flips automatically, with no per-screen changes needed.
// The brand hues (blue/red/success/warning) are brightened a notch so their *text*
// uses (links, amounts, error copy — see AGENTS.md on `colors.blue`) stay legible on
// a near-black background; `onAccent` stays pure white since accent fills themselves
// don't change with the scheme.
export const darkColors: ColorTokens = {
  black: '#F2F1ED',
  white: '#1C1C1E',
  red: '#FF6B6B',
  blue: '#7B82FF',
  bg: '#121212',
  surface: '#1C1C1E',
  surfaceAlt: '#232326',
  line: '#323236',
  pill: '#26262A',
  pillBorder: '#3A3A3E',
  textMuted: '#A3A3A8',
  textFaint: '#6E6E73',
  success: '#4CAF50',
  warning: '#D9A441',
  danger: '#FF6B6B',
  overlay: 'rgba(0,0,0,0.6)',
  blueSoft: '#20224A',
  successSoft: '#1B2E1D',
  warningSoft: '#332A14',
  dangerSoft: '#3A1E1F',
  onAccent: '#FFFFFF',
};
