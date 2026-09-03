import { Ionicons } from '@expo/vector-icons';
import { router, useFocusEffect } from 'expo-router';
import { useCallback, useState } from 'react';
import {
  ActivityIndicator,
  Pressable,
  RefreshControl,
  ScrollView,
  StyleSheet,
  Switch,
  Text,
  View,
} from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';

import { Avatar } from './Avatar';
import { Button } from './Button';
import { Card } from './Card';
import { useAuth } from '../context/AuthContext';
import { getBarber } from '../lib/api/barbers';
import { getBarberShop, toggleAcceptingBarbers } from '../lib/api/barbershops';
import { getSubscriptionStatus } from '../lib/api/subscriptions';
import type { Barber, BarberShop, SubscriptionStatus } from '../lib/types';
import { colors } from '../theme/colors';
import { centeredPage } from '../theme/layout';
import { spacing } from '../theme/spacing';
import { typography } from '../theme/typography';

export function BarberHome() {
  const { session } = useAuth();
  const [barber, setBarber] = useState<Barber | null>(null);
  const [shop, setShop] = useState<BarberShop | null>(null);
  const [subscriptionStatus, setSubscriptionStatus] = useState<SubscriptionStatus | null>(null);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [togglingAccepting, setTogglingAccepting] = useState(false);

  const load = useCallback(async () => {
    if (!session) return;
    try {
      const barberData = await getBarber(session.userId);
      setBarber(barberData);
      if (barberData.barberShopId) {
        setShop(await getBarberShop(barberData.barberShopId));
      } else {
        setShop(null);
      }
      setSubscriptionStatus((await getSubscriptionStatus()).status);
    } catch {
      // A 401 here means the session was stale/invalid; AuthContext's
      // unauthorized handler already clears it and redirects to login.
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  }, [session]);

  useFocusEffect(
    useCallback(() => {
      load();
    }, [load]),
  );

  const isOwner = shop && session ? shop.ownerId === session.userId : false;

  async function handleToggleAccepting() {
    if (!shop) return;
    setTogglingAccepting(true);
    try {
      setShop(await toggleAcceptingBarbers(shop.id));
    } finally {
      setTogglingAccepting(false);
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
      <ScrollView
        contentContainerStyle={styles.content}
        refreshControl={<RefreshControl refreshing={refreshing} onRefresh={() => { setRefreshing(true); load(); }} />}
      >
        <View style={styles.greetingRow}>
          <Avatar name={session?.name} avatarBase64={session?.avatarBase64} size={40} tone="black" />
          <View>
            <Text style={styles.greeting}>Olá, {session?.name?.split(' ')[0]}</Text>
            <Text style={styles.title}>{isOwner ? 'Minha barbearia' : 'Painel do barbeiro'}</Text>
          </View>
        </View>

        {subscriptionStatus && subscriptionStatus !== 'ACTIVE' ? (
          <Pressable onPress={() => router.push('/(app)/subscription')}>
            <Card style={[styles.card, styles.subscriptionCard]}>
              <Ionicons name="lock-closed-outline" size={20} color={colors.red} />
              <View style={{ flex: 1 }}>
                <Text style={styles.cardLabel}>Assinatura</Text>
                <Text style={styles.cardValue}>
                  {subscriptionStatus === 'PENDING_CONFIRMATION'
                    ? 'Pagamento em análise — toque para ver detalhes.'
                    : 'Assine para gerenciar serviços, produtos e agendamentos.'}
                </Text>
              </View>
              <Ionicons name="chevron-forward" size={18} color={colors.textFaint} />
            </Card>
          </Pressable>
        ) : null}

        {barber ? (
          <Pressable onPress={() => router.push('/(app)/availability')}>
            <Card style={styles.card}>
              <View style={styles.statusRow}>
                <View style={{ flex: 1 }}>
                  <Text style={styles.cardLabel}>Disponibilidade</Text>
                  <Text
                    style={[
                      styles.cardValue,
                      { color: barber.available ? colors.success : colors.textMuted },
                    ]}
                  >
                    {barber.available ? 'Disponível' : 'Indisponível'} ·{' '}
                    {String(barber.workStartHour).padStart(2, '0')}h–
                    {String(barber.workEndHour).padStart(2, '0')}h
                  </Text>
                </View>
                <Ionicons name="chevron-forward" size={18} color={colors.textFaint} />
              </View>
            </Card>
          </Pressable>
        ) : null}

        {shop ? (
          <Card style={styles.card}>
            <Text style={styles.cardLabel}>{isOwner ? 'Sua barbearia' : 'Você faz parte de'}</Text>
            <Text style={styles.shopName}>{shop.name}</Text>
            {shop.address ? <Text style={styles.cardValue}>{shop.address}</Text> : null}

            {isOwner ? (
              <View style={styles.toggleRow}>
                <Text style={styles.cardValue}>Aceitando novos barbeiros</Text>
                <Switch
                  value={shop.acceptingBarbers}
                  onValueChange={handleToggleAccepting}
                  disabled={togglingAccepting}
                  trackColor={{ false: colors.pillBorder, true: colors.blue }}
                />
              </View>
            ) : null}
          </Card>
        ) : (
          <Card style={styles.card}>
            <Text style={styles.cardLabel}>Barbearia</Text>
            <Text style={styles.cardValue}>
              Você ainda não faz parte de uma barbearia. Se você solicitou a criação de uma, ela
              está aguardando aprovação do administrador.
            </Text>
            <Button
              title="Solicitar criação de barbearia"
              variant="outline"
              onPress={() => router.push('/(auth)/register-shop')}
              style={styles.cardButton}
            />
          </Card>
        )}
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
    ...centeredPage,
  },
  greetingRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: spacing.md,
    marginBottom: spacing.lg,
  },
  greeting: {
    color: colors.textMuted,
    fontSize: 14,
  },
  title: {
    ...typography.h1,
    color: colors.black,
    marginTop: 2,
  },
  card: {
    marginBottom: spacing.md,
  },
  subscriptionCard: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: spacing.sm,
    borderColor: colors.red,
    borderWidth: 1,
  },
  statusRow: {
    flexDirection: 'row',
    alignItems: 'center',
  },
  cardLabel: {
    ...typography.label,
    color: colors.textMuted,
    marginBottom: 4,
  },
  cardValue: {
    fontSize: 14,
    color: colors.black,
  },
  shopName: {
    fontFamily: typography.h2.fontFamily,
    fontSize: 18,
    color: colors.black,
    marginBottom: 4,
  },
  toggleRow: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    marginTop: spacing.sm,
  },
  cardButton: {
    marginTop: spacing.md,
  },
});
