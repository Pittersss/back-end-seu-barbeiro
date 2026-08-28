import { router } from 'expo-router';
import { useState } from 'react';
import { Pressable, StyleSheet, Text, View } from 'react-native';

import { Button } from '../../components/Button';
import { Screen } from '../../components/Screen';
import { colors } from '../../theme/colors';
import { spacing } from '../../theme/spacing';
import { typography } from '../../theme/typography';

type RoleOption = 'CLIENT' | 'BARBER' | 'OWNER';

const OPTIONS: { value: RoleOption; label: string }[] = [
  { value: 'CLIENT', label: 'Cliente' },
  { value: 'BARBER', label: 'Barbeiro' },
  { value: 'OWNER', label: 'Dono Barbearia' },
];

export default function RegisterRoleScreen() {
  const [selected, setSelected] = useState<RoleOption | null>(null);

  return (
    <Screen center>
      <Text style={styles.title}>CADASTRE-SE</Text>
      <Text style={styles.subtitle}>Você vai se cadastrar como:</Text>

      <View style={styles.options}>
        {OPTIONS.map((option) => {
          const isSelected = option.value === selected;
          return (
            <Pressable
              key={option.value}
              style={styles.optionRow}
              onPress={() => setSelected(option.value)}
            >
              <View style={[styles.radio, isSelected && styles.radioSelected]}>
                {isSelected ? <View style={styles.radioDot} /> : null}
              </View>
              <Text style={styles.optionLabel}>{option.label}</Text>
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

const styles = StyleSheet.create({
  title: {
    ...typography.h1,
    textAlign: 'center',
    color: colors.black,
    marginBottom: spacing.lg,
  },
  subtitle: {
    fontSize: 15,
    color: colors.black,
    marginBottom: spacing.lg,
  },
  options: {
    marginBottom: spacing.xl,
  },
  optionRow: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingVertical: spacing.sm,
  },
  radio: {
    width: 20,
    height: 20,
    borderRadius: 10,
    borderWidth: 1.5,
    borderColor: colors.black,
    alignItems: 'center',
    justifyContent: 'center',
    marginRight: spacing.sm,
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
  optionLabel: {
    fontSize: 15,
    color: colors.black,
  },
  button: {
    alignSelf: 'stretch',
  },
});
