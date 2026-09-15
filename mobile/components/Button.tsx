import { ActivityIndicator, Pressable, Text, type StyleProp, type ViewStyle } from 'react-native';

import { fonts } from '../theme/typography';
import { radius, spacing } from '../theme/spacing';
import { useThemeColors } from '../theme/ThemeContext';
import { useThemedStyles } from '../theme/useThemedStyles';

interface ButtonProps {
  title: string;
  onPress: () => void;
  loading?: boolean;
  disabled?: boolean;
  variant?: 'primary' | 'outline' | 'ghost';
  size?: 'md' | 'sm';
  style?: StyleProp<ViewStyle>;
}

export function Button({
  title,
  onPress,
  loading,
  disabled,
  variant = 'primary',
  size = 'md',
  style,
}: ButtonProps) {
  const colors = useThemeColors();
  const styles = useThemedStyles((colors) => ({
    base: {
      height: 52,
      borderRadius: radius.pill,
      alignItems: 'center',
      justifyContent: 'center',
      paddingHorizontal: spacing.lg,
    },
    sm: {
      height: 40,
      paddingHorizontal: spacing.md,
    },
    primary: {
      backgroundColor: colors.blue,
    },
    outline: {
      backgroundColor: colors.white,
      borderWidth: 1.5,
      borderColor: colors.black,
    },
    ghost: {
      backgroundColor: 'transparent',
    },
    disabled: {
      opacity: 0.4,
    },
    pressed: {
      opacity: 0.9,
      transform: [{ scale: 0.985 }],
    },
    text: {
      fontFamily: fonts.headingMedium,
      fontSize: 15,
      letterSpacing: 1,
      textTransform: 'uppercase',
    },
    textSm: {
      fontSize: 13,
      letterSpacing: 0.5,
    },
    textPrimary: {
      color: colors.onAccent,
    },
    textDark: {
      color: colors.black,
    },
  }));
  const isOutline = variant === 'outline';
  const isGhost = variant === 'ghost';
  const spinnerColor = variant === 'primary' ? colors.onAccent : colors.black;
  return (
    <Pressable
      onPress={onPress}
      disabled={disabled || loading}
      style={({ pressed }) => [
        styles.base,
        size === 'sm' && styles.sm,
        isOutline && styles.outline,
        isGhost && styles.ghost,
        !isOutline && !isGhost && styles.primary,
        (disabled || loading) && styles.disabled,
        pressed && !disabled && !loading && styles.pressed,
        style,
      ]}
    >
      {loading ? (
        <ActivityIndicator color={spinnerColor} />
      ) : (
        <Text
          style={[
            styles.text,
            size === 'sm' && styles.textSm,
            variant === 'primary' ? styles.textPrimary : styles.textDark,
          ]}
        >
          {title}
        </Text>
      )}
    </Pressable>
  );
}
