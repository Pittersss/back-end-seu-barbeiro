import type { ReactNode } from 'react';
import { Text, View } from 'react-native';

import { typography } from '../theme/typography';
import { spacing } from '../theme/spacing';
import { useThemedStyles } from '../theme/useThemedStyles';

interface SectionHeaderProps {
  title: string;
  caption?: string;
  action?: ReactNode;
  style?: object;
}

export function SectionHeader({ title, caption, action, style }: SectionHeaderProps) {
  const styles = useThemedStyles((colors) => ({
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
  }));
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
