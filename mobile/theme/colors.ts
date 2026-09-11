export const colors = {
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
} as const;
