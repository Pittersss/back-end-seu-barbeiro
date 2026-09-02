import { router, useLocalSearchParams } from 'expo-router';
import { useState } from 'react';
import { StyleSheet, Text, View } from 'react-native';

import { Button } from '../../components/Button';
import { Input } from '../../components/Input';
import { Logo } from '../../components/Logo';
import { Screen } from '../../components/Screen';
import { useAuth } from '../../context/AuthContext';
import { ApiError } from '../../lib/api';
import { colors } from '../../theme/colors';
import { spacing } from '../../theme/spacing';

type RoleParam = 'CLIENT' | 'BARBER' | 'OWNER';

export default function ConfirmCodeScreen() {
  const { role, email } = useLocalSearchParams<{ role: RoleParam; email: string }>();
  const { verifyEmail, resendCode } = useAuth();
  const [code, setCode] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [info, setInfo] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);
  const [resending, setResending] = useState(false);

  async function handleConfirm() {
    setError(null);
    setInfo(null);
    setLoading(true);
    try {
      await verifyEmail({ email, code });
      if (role === 'OWNER') {
        router.replace('/(auth)/register-shop');
      } else {
        router.replace('/(app)/home');
      }
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Código inválido. Tente novamente.');
    } finally {
      setLoading(false);
    }
  }

  async function handleResend() {
    setError(null);
    setInfo(null);
    setResending(true);
    try {
      await resendCode({ email });
      setInfo('Um novo código foi enviado para o seu e-mail.');
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Não foi possível reenviar o código.');
    } finally {
      setResending(false);
    }
  }

  return (
    <Screen center>
      <View style={styles.logoWrap}>
        <Logo size={140} />
      </View>

      <Text style={styles.subtitle}>Insira o código de confirmação enviado para {email}</Text>

      <Input
        accent="blue"
        placeholder="000000"
        keyboardType="number-pad"
        maxLength={6}
        value={code}
        onChangeText={setCode}
        style={styles.codeInput}
      />

      {error ? <Text style={styles.error}>{error}</Text> : null}
      {info ? <Text style={styles.info}>{info}</Text> : null}

      <Button title="Confirmar" onPress={handleConfirm} loading={loading} disabled={code.length < 4} />

      <Text style={styles.resend} onPress={resending ? undefined : handleResend}>
        {resending ? 'Reenviando...' : 'Reenviar código'}
      </Text>
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
  error: {
    color: colors.red,
    marginTop: spacing.md,
    textAlign: 'center',
  },
  info: {
    color: colors.textMuted,
    marginTop: spacing.md,
    textAlign: 'center',
  },
  resend: {
    color: colors.blue,
    textAlign: 'center',
    textDecorationLine: 'underline',
    marginTop: spacing.lg,
  },
});
