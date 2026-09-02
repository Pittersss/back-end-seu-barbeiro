import { Ionicons } from '@expo/vector-icons';
import { router, useFocusEffect } from 'expo-router';
import { useCallback, useState } from 'react';
import { ActivityIndicator, Pressable, ScrollView, StyleSheet, Text, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';

import { Avatar } from '../../components/Avatar';
import { Button } from '../../components/Button';
import { Card } from '../../components/Card';
import { Input } from '../../components/Input';
import { SectionHeader } from '../../components/SectionHeader';
import { useAuth } from '../../context/AuthContext';
import { ApiError } from '../../lib/api';
import { getBarber, updateBarber } from '../../lib/api/barbers';
import { getBarberShop, updateBarberShop } from '../../lib/api/barbershops';
import { getMe, updateMe } from '../../lib/api/users';
import { pickAvatarBase64 } from '../../lib/avatar';
import type { Barber, BarberShop, UserProfile } from '../../lib/types';
import { colors } from '../../theme/colors';
import { centeredPage } from '../../theme/layout';
import { spacing } from '../../theme/spacing';
import { typography } from '../../theme/typography';

const ROLE_LABELS: Record<string, string> = {
  BARBER: 'Barbeiro',
  ADMIN: 'Administrador',
  CLIENT: 'Cliente',
};

export default function ProfileScreen() {
  const { session, logout, refreshProfile } = useAuth();
  const isBarber = session?.role === 'BARBER';

  const [profile, setProfile] = useState<UserProfile | null>(null);
  const [barber, setBarber] = useState<Barber | null>(null);
  const [shop, setShop] = useState<BarberShop | null>(null);
  const [loading, setLoading] = useState(true);
  const [avatarBusy, setAvatarBusy] = useState(false);

  const [name, setName] = useState('');
  const [phone, setPhone] = useState('');
  const [savingPersonal, setSavingPersonal] = useState(false);

  const [pixKey, setPixKey] = useState('');
  const [delayTolerance, setDelayTolerance] = useState('0');
  const [savingBarber, setSavingBarber] = useState(false);

  const [shopName, setShopName] = useState('');
  const [shopAddress, setShopAddress] = useState('');
  const [shopPhone, setShopPhone] = useState('');
  const [savingShop, setSavingShop] = useState(false);

  const [error, setError] = useState<string | null>(null);
  const [savedMsg, setSavedMsg] = useState<string | null>(null);

  const isOwner = Boolean(shop && session && shop.ownerId === session.userId);

  const flashSaved = useCallback((msg: string) => {
    setSavedMsg(msg);
    setTimeout(() => setSavedMsg(null), 2000);
  }, []);

  useFocusEffect(
    useCallback(() => {
      let cancelled = false;
      (async () => {
        if (!session) return;
        setLoading(true);
        setError(null);
        try {
          const me = await getMe();
          if (cancelled) return;
          setProfile(me);
          setName(me.name);
          setPhone(me.phone ?? '');

          if (session.role === 'BARBER') {
            const b = await getBarber(session.userId);
            if (cancelled) return;
            setBarber(b);
            setPixKey(b.pixKey ?? '');
            setDelayTolerance(String(b.delayTolerance ?? 0));
            if (b.barberShopId) {
              const s = await getBarberShop(b.barberShopId);
              if (cancelled) return;
              setShop(s);
              setShopName(s.name);
              setShopAddress(s.address ?? '');
              setShopPhone(s.phone ?? '');
            }
          }
        } finally {
          if (!cancelled) setLoading(false);
        }
      })();
      return () => {
        cancelled = true;
      };
    }, [session]),
  );

  async function handleChangeAvatar() {
    setError(null);
    setAvatarBusy(true);
    try {
      const base64 = await pickAvatarBase64();
      if (!base64) return;
      const updated = await updateMe({
        name: name || profile?.name || '',
        phone: phone || null,
        avatarBase64: base64,
      });
      setProfile(updated);
      await refreshProfile();
      flashSaved('Foto atualizada');
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Não foi possível atualizar a foto.');
    } finally {
      setAvatarBusy(false);
    }
  }

  async function handleSavePersonal() {
    setError(null);
    setSavingPersonal(true);
    try {
      const updated = await updateMe({
        name,
        phone: phone || null,
        avatarBase64: profile?.avatarBase64 ?? null,
      });
      setProfile(updated);
      await refreshProfile();
      flashSaved('Dados salvos');
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Não foi possível salvar.');
    } finally {
      setSavingPersonal(false);
    }
  }

  async function handleSaveBarber() {
    if (!session || !barber) return;
    setError(null);
    setSavingBarber(true);
    try {
      const updated = await updateBarber(session.userId, {
        name,
        phone: phone || undefined,
        pixKey: pixKey || undefined,
        delayTolerance: Number(delayTolerance) || 0,
        workStartHour: barber.workStartHour,
        workEndHour: barber.workEndHour,
        breakStartHour: barber.breakStartHour,
        breakEndHour: barber.breakEndHour,
      });
      setBarber(updated);
      flashSaved('Dados de barbeiro salvos');
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Não foi possível salvar.');
    } finally {
      setSavingBarber(false);
    }
  }

  async function handleSaveShop() {
    if (!shop) return;
    setError(null);
    setSavingShop(true);
    try {
      const updated = await updateBarberShop(shop.id, {
        name: shopName,
        address: shopAddress || undefined,
        phone: shopPhone || undefined,
      });
      setShop(updated);
      flashSaved('Barbearia salva');
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Não foi possível salvar.');
    } finally {
      setSavingShop(false);
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
      <ScrollView contentContainerStyle={styles.content}>
        <Text style={styles.title}>Perfil</Text>

        <View style={styles.identity}>
          <Pressable onPress={handleChangeAvatar} style={styles.avatarWrap} disabled={avatarBusy}>
            <Avatar
              name={profile?.name ?? session?.name}
              avatarBase64={profile?.avatarBase64}
              size={92}
              tone="black"
            />
            <View style={styles.avatarBadge}>
              {avatarBusy ? (
                <ActivityIndicator size="small" color={colors.white} />
              ) : (
                <Ionicons name="camera" size={15} color={colors.white} />
              )}
            </View>
          </Pressable>
          <Text style={styles.name}>{profile?.name ?? session?.name}</Text>
          <Text style={styles.role}>{ROLE_LABELS[session?.role ?? 'CLIENT']}</Text>
        </View>

        {error ? <Text style={styles.error}>{error}</Text> : null}
        {savedMsg ? <Text style={styles.saved}>{savedMsg}</Text> : null}

        <SectionHeader title="Dados pessoais" />
        <Card>
          <Input label="Nome" placeholder="Seu nome" value={name} onChangeText={setName} />
          <Input
            label="Telefone"
            placeholder="(00) 00000-0000"
            keyboardType="phone-pad"
            value={phone}
            onChangeText={setPhone}
          />
          <Input label="E-mail" value={profile?.email ?? ''} editable={false} />
          <Button title="Salvar" size="sm" onPress={handleSavePersonal} loading={savingPersonal} />
        </Card>

        {isBarber ? (
          <>
            <SectionHeader title="Barbeiro" />
            <Card>
              <Input
                label="Chave Pix"
                hint="Usada para gerar o QR Code de pagamento dos clientes."
                placeholder="CPF, e-mail, telefone ou chave aleatória"
                value={pixKey}
                onChangeText={setPixKey}
                autoCapitalize="none"
              />
              <Input
                label="Tolerância de atraso (min)"
                hint="Quanto tempo você aguarda um cliente atrasado antes de liberar o horário."
                keyboardType="number-pad"
                value={delayTolerance}
                onChangeText={setDelayTolerance}
              />
              <Button title="Salvar" size="sm" onPress={handleSaveBarber} loading={savingBarber} />
            </Card>

            <SectionHeader title="Agenda" />
            <Card padded={false}>
              <Pressable style={styles.navRow} onPress={() => router.push('/(app)/availability')}>
                <Ionicons name="time-outline" size={20} color={colors.black} />
                <View style={styles.navText}>
                  <Text style={styles.navTitle}>Disponibilidade e horários</Text>
                  <Text style={styles.navHint}>
                    {barber?.available ? 'Disponível' : 'Indisponível'} ·{' '}
                    {String(barber?.workStartHour ?? 9).padStart(2, '0')}h–
                    {String(barber?.workEndHour ?? 18).padStart(2, '0')}h
                  </Text>
                </View>
                <Ionicons name="chevron-forward" size={18} color={colors.textFaint} />
              </Pressable>
              <View style={styles.navDivider} />
              <Pressable style={styles.navRow} onPress={() => router.push('/(app)/blocked-clients')}>
                <Ionicons name="person-remove-outline" size={20} color={colors.black} />
                <View style={styles.navText}>
                  <Text style={styles.navTitle}>Clientes bloqueados</Text>
                  <Text style={styles.navHint}>Quem não pode agendar com você</Text>
                </View>
                <Ionicons name="chevron-forward" size={18} color={colors.textFaint} />
              </Pressable>
            </Card>
          </>
        ) : null}

        {isOwner ? (
          <>
            <SectionHeader title="Minha barbearia" />
            <Card>
              <Input label="Nome da barbearia" value={shopName} onChangeText={setShopName} />
              <Input label="Endereço" value={shopAddress} onChangeText={setShopAddress} />
              <Input
                label="Telefone da barbearia"
                keyboardType="phone-pad"
                value={shopPhone}
                onChangeText={setShopPhone}
              />
              <Button title="Salvar" size="sm" onPress={handleSaveShop} loading={savingShop} />
            </Card>
          </>
        ) : null}

        <Button title="Sair" variant="outline" onPress={logout} style={styles.logoutButton} />
      </ScrollView>
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
    padding: spacing.lg,
    paddingBottom: spacing.xxl,
    ...centeredPage,
  },
  title: {
    ...typography.h1,
    color: colors.black,
    marginBottom: spacing.lg,
  },
  identity: {
    alignItems: 'center',
  },
  avatarWrap: {
    marginBottom: spacing.sm,
  },
  avatarBadge: {
    position: 'absolute',
    right: -2,
    bottom: -2,
    width: 30,
    height: 30,
    borderRadius: 15,
    backgroundColor: colors.blue,
    alignItems: 'center',
    justifyContent: 'center',
    borderWidth: 2,
    borderColor: colors.white,
  },
  name: {
    fontFamily: typography.h2.fontFamily,
    fontSize: 18,
    color: colors.black,
  },
  role: {
    fontSize: 13,
    color: colors.textMuted,
    marginTop: 2,
  },
  error: {
    color: colors.red,
    textAlign: 'center',
    marginTop: spacing.md,
  },
  saved: {
    color: colors.success,
    textAlign: 'center',
    marginTop: spacing.md,
  },
  navRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: spacing.md,
    padding: spacing.md,
  },
  navText: {
    flex: 1,
  },
  navTitle: {
    fontSize: 15,
    color: colors.black,
  },
  navHint: {
    fontSize: 12,
    color: colors.textMuted,
    marginTop: 2,
  },
  navDivider: {
    height: 1,
    backgroundColor: colors.line,
    marginLeft: spacing.md + 20 + spacing.md,
  },
  logoutButton: {
    marginTop: spacing.xl,
  },
});
