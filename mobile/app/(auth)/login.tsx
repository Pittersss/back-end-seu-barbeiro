import { Ionicons } from '@expo/vector-icons';
import { Link, router } from 'expo-router';
import { useState } from 'react';
import { Pressable, Text, View } from 'react-native';

import { Button } from '../../components/Button';
import { Card } from '../../components/Card';
import { Input } from '../../components/Input';
import { Logo } from '../../components/Logo';
import { Screen } from '../../components/Screen';
import { useAuth } from '../../context/AuthContext';
import { ApiError } from '../../lib/api';
import { radius, spacing } from '../../theme/spacing';
import { useTheme } from '../../theme/ThemeContext';
import { typography } from '../../theme/typography';
import { useThemedStyles } from '../../theme/useThemedStyles';

export default function LoginScreen() {
  const { colors, isDark, toggleScheme } = useTheme();
  const styles = useThemedStyles((colors) => ({
    hero: {
      alignItems: 'center',
      marginBottom: spacing.xl,
    },
    title: {
      ...typography.display,
      color: colors.black,
      textAlign: 'center',
      marginTop: spacing.lg,
    },
    subtitle: {
      ...typography.bodyMuted,
      color: colors.textMuted,
      textAlign: 'center',
      marginTop: spacing.xs,
    },
    card: {
      alignSelf: 'stretch',
      padding: spacing.lg,
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
    themeToggle: {
      flexDirection: 'row',
      alignItems: 'center',
      gap: 6,
      paddingHorizontal: spacing.sm,
      paddingVertical: 7,
      borderRadius: radius.pill,
      backgroundColor: colors.pill,
      borderWidth: 1,
      borderColor: colors.pillBorder,
    },
    themeToggleText: {
      ...typography.caption,
      color: colors.black,
    },
  }));
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
    <Screen
      center
      topRight={
        <Pressable onPress={toggleScheme} style={styles.themeToggle} hitSlop={8}>
          <Ionicons name={isDark ? 'sunny-outline' : 'moon-outline'} size={15} color={colors.black} />
          <Text style={styles.themeToggleText}>{isDark ? 'Modo claro' : 'Modo escuro'}</Text>
        </Pressable>
      }
    >
      <View style={styles.hero}>
        <Logo size={200} />
        <Text style={styles.title}>Bem-vindo de volta</Text>
        <Text style={styles.subtitle}>Entre para agendar o seu próximo corte</Text>
      </View>

      <Card style={styles.card}>
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
      </Card>

      <View style={styles.footer}>
        <Text style={styles.footerText}>Não tem uma conta? </Text>
        <Link href="/(auth)/register-role" style={styles.link}>
          Cadastre-se
        </Link>
      </View>
    </Screen>
  );
}
