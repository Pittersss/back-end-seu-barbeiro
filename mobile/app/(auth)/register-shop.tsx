import { router } from 'expo-router';
import { useState } from 'react';
import { StyleSheet, Text, View } from 'react-native';

import { Button } from '../../components/Button';
import { Input } from '../../components/Input';
import { Screen } from '../../components/Screen';
import { requestBarberShopCreation } from '../../lib/api/barbershops';
import { ApiError } from '../../lib/api';
import { colors } from '../../theme/colors';
import { spacing } from '../../theme/spacing';
import { typography } from '../../theme/typography';

export default function RegisterShopScreen() {
  const [shopName, setShopName] = useState('');
  const [shopAddress, setShopAddress] = useState('');
  const [shopPhone, setShopPhone] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);
  const [submitted, setSubmitted] = useState(false);

  async function handleSubmit() {
    setError(null);
    setLoading(true);
    try {
      await requestBarberShopCreation({ shopName, shopAddress, shopPhone });
      setSubmitted(true);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Não foi possível enviar a solicitação.');
    } finally {
      setLoading(false);
    }
  }

  if (submitted) {
    return (
      <Screen center>
        <Text style={styles.title}>Solicitação enviada</Text>
        <Text style={styles.subtitle}>
          Seu pedido para criar a barbearia &quot;{shopName}&quot; foi enviado e está aguardando
          aprovação. Você já pode entrar no app.
        </Text>
        <Button title="Ir para o início" onPress={() => router.replace('/(app)/home')} />
      </Screen>
    );
  }

  return (
    <Screen center>
      <Text style={styles.title}>Dados da barbearia</Text>

      <Input accent="blue" placeholder="Nome da barbearia" value={shopName} onChangeText={setShopName} />
      <Input accent="blue" placeholder="Endereço" value={shopAddress} onChangeText={setShopAddress} />
      <Input
        accent="blue"
        placeholder="Telefone"
        keyboardType="phone-pad"
        value={shopPhone}
        onChangeText={setShopPhone}
      />

      {error ? <Text style={styles.error}>{error}</Text> : null}

      <Button title="Enviar solicitação" onPress={handleSubmit} loading={loading} disabled={!shopName} />

      <View style={styles.skipWrap}>
        <Text style={styles.skipText} onPress={() => router.replace('/(app)/home')}>
          Fazer isso depois
        </Text>
      </View>
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
    textAlign: 'center',
    marginBottom: spacing.lg,
  },
  error: {
    color: colors.red,
    marginBottom: spacing.md,
    textAlign: 'center',
  },
  skipWrap: {
    marginTop: spacing.lg,
    alignItems: 'center',
  },
  skipText: {
    color: colors.textMuted,
    textDecorationLine: 'underline',
  },
});
