export const colors = {
  black: '#0A0A0A',
  white: '#FFFFFF',
  red: '#C1272D',
  blue: '#2F37C9',
  pill: '#E9E9EC',
  pillBorder: '#DADADA',
  textMuted: '#8A8A8E',
  textFaint: '#B8B8BC',
  success: '#2E7D32',
  warning: '#B8860B',
  danger: '#C1272D',
  overlay: 'rgba(10,10,10,0.4)',
} as const;

export const statusColors: Record<string, string> = {
  PENDING: colors.warning,
  CONFIRMED: colors.blue,
  COMPLETED: colors.success,
  CANCELLED: colors.danger,
};
