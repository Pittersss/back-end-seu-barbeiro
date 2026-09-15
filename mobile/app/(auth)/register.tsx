import { router, useLocalSearchParams } from 'expo-router';
import { useState } from 'react';
import { Text, View } from 'react-native';

import { Button } from '../../components/Button';
import { Card } from '../../components/Card';
import { Input } from '../../components/Input';
import { Logo } from '../../components/Logo';
import { Screen } from '../../components/Screen';
import { useAuth } from '../../context/AuthContext';
import { ApiError } from '../../lib/api';
import { spacing } from '../../theme/spacing';
import { typography } from '../../theme/typography';
import { useThemedStyles } from '../../theme/useThemedStyles';

type RoleParam = 'CLIENT' | 'BARBER' | 'OWNER';

const ROLE_LABELS: Record<RoleParam, string> = {
  CLIENT: 'Cliente',
  BARBER: 'Barbeiro',
  OWNER: 'Dono de barbearia',
};

export default function RegisterScreen() {
  const styles = useThemedStyles((colors) => ({
    hero: {
      alignItems: 'center',
      marginBottom: spacing.lg,
    },
    title: {
      ...typography.display,
      textAlign: 'center',
      color: colors.black,
      marginTop: spacing.md,
    },
    subtitle: {
      ...typography.bodyMuted,
      textAlign: 'center',
      color: colors.textMuted,
      marginTop: spacing.xs,
    },
    card: {
      padding: spacing.lg,
    },
    error: {
      color: colors.red,
      marginBottom: spacing.md,
      textAlign: 'center',
    },
  }));
  const { role } = useLocalSearchParams<{ role: RoleParam }>();
  const { registerClient, registerBarber } = useAuth();
  const isBarberLike = role === 'BARBER' || role === 'OWNER';

  const [name, setName] = useState('');
  const [email, setEmail] = useState('');
  const [phone, setPhone] = useState('');
  const [pixKey, setPixKey] = useState('');
  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  const passwordsMismatch = confirmPassword.length > 0 && password !== confirmPassword;
  const canSubmit =
    !!name && !!email && password.length >= 8 && password === confirmPassword;

  async function handleSubmit() {
    if (password !== confirmPassword) {
      setError('As senhas não conferem.');
      return;
    }

    setError(null);
    setLoading(true);
    try {
      if (isBarberLike) {
        await registerBarber({
          name,
          email: email.trim(),
          password,
          phone: phone || undefined,
          pixKey: pixKey || undefined,
        });
      } else {
        await registerClient({
          name,
          email: email.trim(),
          password,
          phone: phone || undefined,
        });
      }
      router.push({ pathname: '/(auth)/confirm-code', params: { role, email: email.trim() } });
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Não foi possível concluir o cadastro.');
    } finally {
      setLoading(false);
    }
  }

  return (
    <Screen>
      <View style={styles.hero}>
        <Logo size={120} />
        <Text style={styles.title}>Cadastre-se</Text>
        <Text style={styles.subtitle}>
          {role ? `Criando conta de ${ROLE_LABELS[role].toLowerCase()}` : 'Crie sua conta para começar'}
        </Text>
      </View>

      <Card style={styles.card}>
        <Input
          accent="blue"
          icon="person-outline"
          placeholder="Nome completo"
          value={name}
          onChangeText={setName}
        />
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
          icon="call-outline"
          placeholder="Telefone (opcional)"
          keyboardType="phone-pad"
          value={phone}
          onChangeText={setPhone}
        />
        {isBarberLike ? (
          <Input
            accent="blue"
            icon="key-outline"
            placeholder="Chave Pix (opcional)"
            value={pixKey}
            onChangeText={setPixKey}
          />
        ) : null}
        <Input
          accent="blue"
          icon="lock-closed-outline"
          placeholder="Crie uma senha"
          secureTextEntry
          hint="Mínimo de 8 caracteres"
          value={password}
          onChangeText={setPassword}
        />
        <Input
          accent="blue"
          icon="lock-closed-outline"
          placeholder="Confirme sua senha"
          secureTextEntry
          value={confirmPassword}
          onChangeText={setConfirmPassword}
          error={passwordsMismatch ? 'As senhas não conferem.' : undefined}
        />

        {error ? <Text style={styles.error}>{error}</Text> : null}

        <Button title="Registrar" onPress={handleSubmit} loading={loading} disabled={!canSubmit} />
      </Card>
    </Screen>
  );
}
