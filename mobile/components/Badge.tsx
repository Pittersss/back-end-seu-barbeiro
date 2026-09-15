import { Text, View } from 'react-native';

import type { ColorTokens } from '../theme/colors';
import { fonts } from '../theme/typography';
import { radius, spacing } from '../theme/spacing';
import { useThemeColors } from '../theme/ThemeContext';
import { useThemedStyles } from '../theme/useThemedStyles';

const STATUS_LABELS: Record<string, string> = {
  PENDING: 'Pendente',
  CONFIRMED: 'Confirmado',
  COMPLETED: 'Concluído',
  CANCELLED: 'Cancelado',
};

function statusStyle(colors: ColorTokens, status: string): { fg: string; bg: string } {
  switch (status) {
    case 'PENDING':
      return { fg: colors.warning, bg: colors.warningSoft };
    case 'CONFIRMED':
      return { fg: colors.blue, bg: colors.blueSoft };
    case 'COMPLETED':
      return { fg: colors.success, bg: colors.successSoft };
    case 'CANCELLED':
      return { fg: colors.danger, bg: colors.dangerSoft };
    default:
      return { fg: colors.textMuted, bg: colors.pill };
  }
}

export function StatusBadge({ status }: { status: string }) {
  const colors = useThemeColors();
  const styles = useThemedStyles(() => ({
    badge: {
      flexDirection: 'row',
      alignItems: 'center',
      alignSelf: 'flex-start',
      borderRadius: radius.pill,
      paddingHorizontal: spacing.sm,
      paddingVertical: 5,
    },
    dot: {
      width: 6,
      height: 6,
      borderRadius: 3,
      marginRight: 6,
    },
    text: {
      fontFamily: fonts.headingMedium,
      fontSize: 11,
      letterSpacing: 0.5,
      textTransform: 'uppercase',
    },
  }));
  const tone = statusStyle(colors, status);
  return (
    <View style={[styles.badge, { backgroundColor: tone.bg }]}>
      <View style={[styles.dot, { backgroundColor: tone.fg }]} />
      <Text style={[styles.text, { color: tone.fg }]}>{STATUS_LABELS[status] ?? status}</Text>
    </View>
  );
}
