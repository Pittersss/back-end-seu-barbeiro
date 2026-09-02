import { Ionicons } from '@expo/vector-icons';
import { useState } from 'react';
import { StyleSheet, Text, TextInput, View, type TextInputProps, type TextStyle } from 'react-native';

type FocusArg = Parameters<NonNullable<TextInputProps['onFocus']>>[0];
type BlurArg = Parameters<NonNullable<TextInputProps['onBlur']>>[0];

import { colors } from '../theme/colors';
import { typography } from '../theme/typography';
import { radius, spacing } from '../theme/spacing';

interface InputProps extends TextInputProps {
  /** Focus-accent color. Kept for back-compat with older call sites. */
  accent?: 'blue' | 'red';
  label?: string;
  hint?: string;
  error?: string;
  icon?: keyof typeof Ionicons.glyphMap;
}

// react-native-web renders TextInput as a plain <input>, and browsers draw
// their own focus ring around it by default. `outlineStyle: 'none'` is a
// react-native-web-only style key (ignored on native) that suppresses it;
// RN's own type defs don't know about it, hence the cast.
const noWebOutline = { outlineStyle: 'none' } as unknown as TextStyle;

export function Input({
  accent = 'blue',
  label,
  hint,
  error,
  icon,
  style,
  editable = true,
  onFocus,
  onBlur,
  ...rest
}: InputProps) {
  const [focused, setFocused] = useState(false);
  const accentColor = accent === 'red' ? colors.red : colors.blue;

  function handleFocus(e: FocusArg) {
    setFocused(true);
    onFocus?.(e);
  }
  function handleBlur(e: BlurArg) {
    setFocused(false);
    onBlur?.(e);
  }

  return (
    <View style={styles.wrap}>
      {label ? <Text style={styles.label}>{label}</Text> : null}
      <View
        style={[
          styles.field,
          !editable && styles.fieldDisabled,
          focused && { borderColor: accentColor },
          error ? styles.fieldError : null,
        ]}
      >
        {icon ? (
          <Ionicons
            name={icon}
            size={18}
            color={focused ? accentColor : colors.textMuted}
            style={styles.icon}
          />
        ) : null}
        <TextInput
          placeholderTextColor={colors.textMuted}
          editable={editable}
          onFocus={handleFocus}
          onBlur={handleBlur}
          style={[styles.input, noWebOutline, style]}
          {...rest}
        />
      </View>
      {error ? (
        <Text style={styles.errorText}>{error}</Text>
      ) : hint ? (
        <Text style={styles.hintText}>{hint}</Text>
      ) : null}
    </View>
  );
}

const styles = StyleSheet.create({
  wrap: {
    marginBottom: spacing.md,
  },
  label: {
    ...typography.label,
    color: colors.textMuted,
    marginBottom: 6,
  },
  field: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: colors.surfaceAlt,
    borderRadius: radius.field,
    borderWidth: 1.5,
    borderColor: colors.line,
    height: 52,
    paddingHorizontal: spacing.md,
  },
  fieldDisabled: {
    opacity: 0.6,
  },
  fieldError: {
    borderColor: colors.danger,
  },
  icon: {
    marginRight: spacing.sm,
  },
  input: {
    flex: 1,
    fontSize: 15,
    color: colors.black,
    height: '100%',
  },
  hintText: {
    fontSize: 12,
    color: colors.textMuted,
    marginTop: 5,
    marginLeft: 2,
  },
  errorText: {
    fontSize: 12,
    color: colors.danger,
    marginTop: 5,
    marginLeft: 2,
  },
});
