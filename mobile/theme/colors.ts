export const colors = {
  black: '#0A0A0A',
  white: '#FFFFFF',
  red: '#C1272D',
  blue: '#2F37C9',
  // Neutral surfaces / hairlines — same greyscale family as `pill`, pulled out
  // so cards and section backgrounds read as one system.
  surface: '#FFFFFF',
  surfaceAlt: '#F5F5F7',
  line: '#ECECEF',
  pill: '#E9E9EC',
  pillBorder: '#DADADA',
  textMuted: '#8A8A8E',
  textFaint: '#B8B8BC',
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
