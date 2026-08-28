import { StyleSheet, TextInput, View, type TextInputProps, type TextStyle } from 'react-native';

import { colors } from '../theme/colors';
import { radius, spacing } from '../theme/spacing';

interface InputProps extends TextInputProps {
  accent?: 'blue' | 'red';
}

// react-native-web renders TextInput as a plain <input>, and browsers draw
// their own focus ring around it by default. `outlineStyle: 'none'` is a
// react-native-web-only style key (ignored on native) that suppresses it;
// RN's own type defs don't know about it, hence the cast.
const noWebOutline = { outlineStyle: 'none' } as unknown as TextStyle;

export function Input({ accent = 'blue', style, ...rest }: InputProps) {
  return (
    <View style={styles.wrap}>
      <View style={[styles.chip, { backgroundColor: accent === 'blue' ? colors.blue : colors.red }]} />
      <TextInput
        placeholderTextColor={colors.textMuted}
        style={[styles.input, noWebOutline, style]}
        {...rest}
      />
    </View>
  );
}

const styles = StyleSheet.create({
  wrap: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: colors.pill,
    borderRadius: radius.pill,
    borderWidth: 1.5,
    borderColor: colors.pillBorder,
    height: 52,
    paddingHorizontal: spacing.sm,
    marginBottom: spacing.md,
  },
  chip: {
    width: 26,
    height: 26,
    borderRadius: 13,
    marginRight: spacing.sm,
  },
  input: {
    flex: 1,
    fontSize: 15,
    color: colors.black,
    paddingRight: spacing.md,
  },
});
