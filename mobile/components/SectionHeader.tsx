import type { ReactNode } from 'react';
import { StyleSheet, Text, View } from 'react-native';

import { colors } from '../theme/colors';
import { typography } from '../theme/typography';
import { spacing } from '../theme/spacing';

interface SectionHeaderProps {
  title: string;
  caption?: string;
  action?: ReactNode;
  style?: object;
}

export function SectionHeader({ title, caption, action, style }: SectionHeaderProps) {
  return (
    <View style={[styles.row, style]}>
      <View style={styles.textWrap}>
        <Text style={styles.title}>{title}</Text>
        {caption ? <Text style={styles.caption}>{caption}</Text> : null}
      </View>
      {action ?? null}
    </View>
  );
}

const styles = StyleSheet.create({
  row: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    marginTop: spacing.lg,
    marginBottom: spacing.sm,
  },
  textWrap: {
    flex: 1,
  },
  title: {
    ...typography.h3,
    color: colors.black,
  },
  caption: {
    fontSize: 12,
    color: colors.textMuted,
    marginTop: 2,
  },
});
