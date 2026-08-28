import { router, useLocalSearchParams } from 'expo-router';
import { useState } from 'react';
import { StyleSheet, Text, View } from 'react-native';

import { Button } from '../../components/Button';
import { Input } from '../../components/Input';
import { Logo } from '../../components/Logo';
import { Screen } from '../../components/Screen';
import { colors } from '../../theme/colors';
import { spacing } from '../../theme/spacing';

type RoleParam = 'CLIENT' | 'BARBER' | 'OWNER';

export default function ConfirmCodeScreen() {
  const { role } = useLocalSearchParams<{ role: RoleParam }>();
  const [code, setCode] = useState('');

  function handleConfirm() {
    if (role === 'OWNER') {
      router.replace('/(auth)/register-shop');
    } else {
      router.replace('/(app)/home');
    }
  }

  return (
    <Screen center>
      <View style={styles.logoWrap}>
        <Logo size={140} />
      </View>

      <Text style={styles.subtitle}>Insira o código de confirmação abaixo</Text>

      <Input
        accent="blue"
        placeholder="000000"
        keyboardType="number-pad"
        maxLength={6}
        value={code}
        onChangeText={setCode}
        style={styles.codeInput}
      />

      <Button title="Confirmar" onPress={handleConfirm} disabled={code.length < 4} />
    </Screen>
  );
}

const styles = StyleSheet.create({
  logoWrap: {
    alignItems: 'center',
    marginBottom: spacing.xl,
  },
  subtitle: {
    fontSize: 15,
    color: colors.black,
    textAlign: 'center',
    marginBottom: spacing.lg,
  },
  codeInput: {
    textAlign: 'center',
    letterSpacing: 6,
  },
});
