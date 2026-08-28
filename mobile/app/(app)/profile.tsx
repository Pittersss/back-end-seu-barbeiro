import { useFocusEffect } from 'expo-router';
import { useCallback, useState } from 'react';
import { ActivityIndicator, StyleSheet, Text, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';

import { Button } from '../../components/Button';
import { Input } from '../../components/Input';
import { useAuth } from '../../context/AuthContext';
import { ApiError } from '../../lib/api';
import { getBarber, updateBarber } from '../../lib/api/barbers';
import type { Barber } from '../../lib/types';
import { colors } from '../../theme/colors';
import { centeredPage } from '../../theme/layout';
import { radius, spacing } from '../../theme/spacing';
import { typography } from '../../theme/typography';

export default function ProfileScreen() {
  const { session, logout } = useAuth();
  const isBarber = session?.role === 'BARBER';

  const [barber, setBarber] = useState<Barber | null>(null);
  const [loading, setLoading] = useState(isBarber);
  const [name, setName] = useState('');
  const [phone, setPhone] = useState('');
  const [pixKey, setPixKey] = useState('');
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [saved, setSaved] = useState(false);

  useFocusEffect(
    useCallback(() => {
      if (!isBarber || !session) return;
      (async () => {
        setLoading(true);
        const data = await getBarber(session.userId);
        setBarber(data);
        setName(data.name);
        setPhone(data.phone ?? '');
        setPixKey(data.pixKey ?? '');
        setLoading(false);
      })();
    }, [isBarber, session]),
  );

  async function handleSave() {
    if (!session || !barber) return;
    setError(null);
    setSaving(true);
    setSaved(false);
    try {
      const updated = await updateBarber(session.userId, {
        name,
        phone: phone || undefined,
        pixKey: pixKey || undefined,
        delayTolerance: barber.delayTolerance,
      });
      setBarber(updated);
      setSaved(true);
      setTimeout(() => setSaved(false), 2000);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Não foi possível salvar.');
    } finally {
      setSaving(false);
    }
  }

  if (loading) {
    return (
      <SafeAreaView style={styles.safe}>
        <ActivityIndicator style={styles.loading} color={colors.black} />
      </SafeAreaView>
    );
  }

  return (
    <SafeAreaView style={styles.safe} edges={['top']}>
      <View style={styles.content}>
        <Text style={styles.title}>Perfil</Text>

        <View style={styles.avatar}>
          <Text style={styles.avatarText}>{session?.name?.[0]?.toUpperCase()}</Text>
        </View>
        <Text style={styles.name}>{session?.name}</Text>
        <Text style={styles.role}>
          {session?.role === 'BARBER' ? 'Barbeiro' : session?.role === 'ADMIN' ? 'Administrador' : 'Cliente'}
        </Text>

        {isBarber ? (
          <View style={styles.form}>
            <Input accent="blue" placeholder="Nome" value={name} onChangeText={setName} />
            <Input accent="blue" placeholder="Telefone" keyboardType="phone-pad" value={phone} onChangeText={setPhone} />
            <Input accent="blue" placeholder="Chave Pix" value={pixKey} onChangeText={setPixKey} />
            {error ? <Text style={styles.error}>{error}</Text> : null}
            {saved ? <Text style={styles.saved}>Salvo!</Text> : null}
            <Button title="Salvar alterações" onPress={handleSave} loading={saving} />
          </View>
        ) : null}

        <Button title="Sair" variant="outline" onPress={logout} style={styles.logoutButton} />
      </View>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  safe: {
    flex: 1,
    backgroundColor: colors.white,
  },
  loading: {
    flex: 1,
  },
  content: {
    flex: 1,
    padding: spacing.lg,
    alignItems: 'center',
    ...centeredPage,
  },
  title: {
    ...typography.h1,
    color: colors.black,
    alignSelf: 'flex-start',
    marginBottom: spacing.lg,
  },
  avatar: {
    width: 84,
    height: 84,
    borderRadius: 42,
    backgroundColor: colors.black,
    alignItems: 'center',
    justifyContent: 'center',
    marginBottom: spacing.sm,
  },
  avatarText: {
    color: colors.white,
    fontFamily: typography.h1.fontFamily,
    fontSize: 32,
  },
  name: {
    fontFamily: typography.h2.fontFamily,
    fontSize: 18,
    color: colors.black,
  },
  role: {
    fontSize: 13,
    color: colors.textMuted,
    marginBottom: spacing.xl,
  },
  form: {
    width: '100%',
    marginBottom: spacing.lg,
  },
  error: {
    color: colors.red,
    marginBottom: spacing.sm,
    textAlign: 'center',
  },
  saved: {
    color: colors.success,
    marginBottom: spacing.sm,
    textAlign: 'center',
  },
  logoutButton: {
    alignSelf: 'stretch',
    marginTop: 'auto',
  },
});
