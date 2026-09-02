import { StyleSheet, Text, View } from 'react-native';

import { colors } from '../theme/colors';
import { fonts } from '../theme/typography';
import { radius, spacing } from '../theme/spacing';

const STATUS_LABELS: Record<string, string> = {
  PENDING: 'Pendente',
  CONFIRMED: 'Confirmado',
  COMPLETED: 'Concluído',
  CANCELLED: 'Cancelado',
};

const STATUS_STYLE: Record<string, { fg: string; bg: string }> = {
  PENDING: { fg: colors.warning, bg: colors.warningSoft },
  CONFIRMED: { fg: colors.blue, bg: colors.blueSoft },
  COMPLETED: { fg: colors.success, bg: colors.successSoft },
  CANCELLED: { fg: colors.danger, bg: colors.dangerSoft },
};

export function StatusBadge({ status }: { status: string }) {
  const tone = STATUS_STYLE[status] ?? { fg: colors.textMuted, bg: colors.pill };
  return (
    <View style={[styles.badge, { backgroundColor: tone.bg }]}>
      <View style={[styles.dot, { backgroundColor: tone.fg }]} />
      <Text style={[styles.text, { color: tone.fg }]}>{STATUS_LABELS[status] ?? status}</Text>
    </View>
  );
}

const styles = StyleSheet.create({
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
});
