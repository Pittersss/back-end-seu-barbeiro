import { Link, router } from 'expo-router';
import { useState } from 'react';
import { StyleSheet, Text, View } from 'react-native';

import { Button } from '../../components/Button';
import { Input } from '../../components/Input';
import { Logo } from '../../components/Logo';
import { Screen } from '../../components/Screen';
import { useAuth } from '../../context/AuthContext';
import { ApiError } from '../../lib/api';
import { colors } from '../../theme/colors';
import { radius, spacing } from '../../theme/spacing';
import { typography } from '../../theme/typography';

export default function LoginScreen() {
  const { login } = useAuth();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  async function handleLogin() {
    setError(null);
    setLoading(true);
    try {
      await login({ email: email.trim(), password });
      router.replace('/(app)/home');
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Não foi possível entrar. Tente novamente.');
    } finally {
      setLoading(false);
    }
  }

  return (
    <Screen center>
      <View style={styles.hero}>
        <Logo size={220} />
      </View>

      <Text style={styles.title}>Bem-vindo de volta</Text>
      <Text style={styles.subtitle}>Entre para agendar o seu próximo corte</Text>

      <View style={styles.form}>
        <Input
          accent="blue"
          icon="mail-outline"
          placeholder="Digite seu e-mail"
          autoCapitalize="none"
          keyboardType="email-address"
          value={email}
          onChangeText={setEmail}
        />
        <Input
          accent="blue"
          icon="lock-closed-outline"
          placeholder="Digite sua senha"
          secureTextEntry
          value={password}
          onChangeText={setPassword}
        />

        {error ? <Text style={styles.error}>{error}</Text> : null}

        <Button
          title="Entrar"
          onPress={handleLogin}
          loading={loading}
          disabled={!email || !password}
        />
      </View>

      <View style={styles.footer}>
        <Text style={styles.footerText}>Não tem uma conta? </Text>
        <Link href="/(auth)/register-role" style={styles.link}>
          Cadastre-se
        </Link>
      </View>
    </Screen>
  );
}

const styles = StyleSheet.create({
  hero: {
    alignSelf: 'stretch',
    alignItems: 'center',
    backgroundColor: colors.blueSoft,
    borderRadius: radius.card,
    paddingVertical: spacing.xl,
    marginBottom: spacing.xl,
  },
  title: {
    ...typography.h1,
    color: colors.black,
    textAlign: 'center',
    marginBottom: spacing.xs,
  },
  subtitle: {
    ...typography.bodyMuted,
    color: colors.textMuted,
    textAlign: 'center',
    marginBottom: spacing.xl,
  },
  form: {
    alignSelf: 'stretch',
  },
  error: {
    color: colors.red,
    marginBottom: spacing.md,
    textAlign: 'center',
  },
  footer: {
    flexDirection: 'row',
    justifyContent: 'center',
    marginTop: spacing.xl,
  },
  footerText: {
    color: colors.textMuted,
  },
  link: {
    color: colors.blue,
    fontWeight: '600',
  },
});
