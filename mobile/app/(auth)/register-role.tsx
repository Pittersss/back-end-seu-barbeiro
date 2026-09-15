import { Ionicons } from '@expo/vector-icons';
import { router } from 'expo-router';
import { useState } from 'react';
import { Pressable, Text, View } from 'react-native';

import { Button } from '../../components/Button';
import { Logo } from '../../components/Logo';
import { Screen } from '../../components/Screen';
import { useThemeColors } from '../../theme/ThemeContext';
import { radius, spacing } from '../../theme/spacing';
import { typography } from '../../theme/typography';
import { useThemedStyles } from '../../theme/useThemedStyles';

type RoleOption = 'CLIENT' | 'BARBER' | 'OWNER';

const OPTIONS: { value: RoleOption; label: string; hint: string; icon: keyof typeof Ionicons.glyphMap }[] = [
  { value: 'CLIENT', label: 'Cliente', hint: 'Quero agendar horários', icon: 'person-outline' },
  { value: 'BARBER', label: 'Barbeiro', hint: 'Trabalho em uma barbearia', icon: 'cut-outline' },
  { value: 'OWNER', label: 'Dono de barbearia', hint: 'Quero cadastrar minha barbearia', icon: 'business-outline' },
];

export default function RegisterRoleScreen() {
  const colors = useThemeColors();
  const styles = useThemedStyles((colors) => ({
    hero: {
      alignItems: 'center',
      marginBottom: spacing.xl,
    },
    title: {
      ...typography.display,
      textAlign: 'center',
      color: colors.black,
      marginTop: spacing.md,
    },
    subtitle: {
      ...typography.bodyMuted,
      color: colors.textMuted,
      textAlign: 'center',
      marginTop: spacing.xs,
    },
    options: {
      gap: spacing.sm,
      marginBottom: spacing.xl,
    },
    option: {
      flexDirection: 'row',
      alignItems: 'center',
      gap: spacing.md,
      backgroundColor: colors.surface,
      borderRadius: radius.card,
      borderWidth: 1.5,
      borderColor: colors.line,
      padding: spacing.md,
    },
    optionSelected: {
      borderColor: colors.blue,
      backgroundColor: colors.blueSoft,
    },
    optionPressed: {
      opacity: 0.9,
      transform: [{ scale: 0.99 }],
    },
    optionIcon: {
      width: 40,
      height: 40,
      borderRadius: 20,
      backgroundColor: colors.surfaceAlt,
      alignItems: 'center',
      justifyContent: 'center',
    },
    optionIconSelected: {
      backgroundColor: colors.surface,
    },
    optionText: {
      flex: 1,
    },
    optionLabel: {
      ...typography.h3,
      color: colors.black,
    },
    optionHint: {
      ...typography.caption,
      color: colors.textMuted,
      marginTop: 2,
    },
    radio: {
      width: 20,
      height: 20,
      borderRadius: 10,
      borderWidth: 1.5,
      borderColor: colors.pillBorder,
      alignItems: 'center',
      justifyContent: 'center',
    },
    radioSelected: {
      borderColor: colors.blue,
    },
    radioDot: {
      width: 10,
      height: 10,
      borderRadius: 5,
      backgroundColor: colors.blue,
    },
    button: {
      alignSelf: 'stretch',
    },
  }));
  const [selected, setSelected] = useState<RoleOption | null>(null);

  return (
    <Screen center>
      <View style={styles.hero}>
        <Logo size={120} />
        <Text style={styles.title}>Cadastre-se</Text>
        <Text style={styles.subtitle}>Como você vai usar o Seu Barbeiro?</Text>
      </View>

      <View style={styles.options}>
        {OPTIONS.map((option) => {
          const isSelected = option.value === selected;
          return (
            <Pressable
              key={option.value}
              style={({ pressed }) => [
                styles.option,
                isSelected && styles.optionSelected,
                pressed && styles.optionPressed,
              ]}
              onPress={() => setSelected(option.value)}
            >
              <View style={[styles.optionIcon, isSelected && styles.optionIconSelected]}>
                <Ionicons
                  name={option.icon}
                  size={20}
                  color={isSelected ? colors.blue : colors.textMuted}
                />
              </View>
              <View style={styles.optionText}>
                <Text style={styles.optionLabel}>{option.label}</Text>
                <Text style={styles.optionHint}>{option.hint}</Text>
              </View>
              <View style={[styles.radio, isSelected && styles.radioSelected]}>
                {isSelected ? <View style={styles.radioDot} /> : null}
              </View>
            </Pressable>
          );
        })}
      </View>

      <Button
        title="Continuar"
        disabled={!selected}
        onPress={() =>
          router.push({ pathname: '/(auth)/register', params: { role: selected! } })
        }
        style={styles.button}
      />
    </Screen>
  );
}
