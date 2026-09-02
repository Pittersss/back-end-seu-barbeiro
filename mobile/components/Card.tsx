import type { ReactNode } from 'react';
import { StyleSheet, View, type StyleProp, type ViewStyle } from 'react-native';

import { colors } from '../theme/colors';
import { shadows } from '../theme/shadows';
import { radius, spacing } from '../theme/spacing';

interface CardProps {
  children: ReactNode;
  style?: StyleProp<ViewStyle>;
  padded?: boolean;
  /** `flat` drops the shadow (for nested / list-embedded cards). */
  variant?: 'elevated' | 'flat';
}

export function Card({ children, style, padded = true, variant = 'elevated' }: CardProps) {
  return (
    <View
      style={[
        styles.card,
        variant === 'elevated' ? shadows.card : undefined,
        padded && styles.padded,
        style,
      ]}
    >
      {children}
    </View>
  );
}

const styles = StyleSheet.create({
  card: {
    backgroundColor: colors.surface,
    borderRadius: radius.card,
    borderWidth: 1,
    borderColor: colors.line,
  },
  padded: {
    padding: spacing.md,
  },
});
