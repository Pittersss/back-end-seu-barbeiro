import { Ionicons } from '@expo/vector-icons';
import { router, useFocusEffect, useLocalSearchParams } from 'expo-router';
import { useCallback, useState } from 'react';
import { ActivityIndicator, Pressable, ScrollView, StyleSheet, Text, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';

import { Avatar } from '../../../components/Avatar';
import { Button } from '../../../components/Button';
import { Card } from '../../../components/Card';
import { SectionHeader } from '../../../components/SectionHeader';
import { ApiError } from '../../../lib/api';
import { getBarberShop, listShopBarbers } from '../../../lib/api/barbershops';
import { listServices } from '../../../lib/api/services';
import { formatCurrency, formatDuration } from '../../../lib/format';
import type { Barber, BarberShop, Service } from '../../../lib/types';
import { colors } from '../../../theme/colors';
import { centeredPage } from '../../../theme/layout';
import { radius, spacing } from '../../../theme/spacing';
import { typography } from '../../../theme/typography';

export default function ShopDetailScreen() {
  const { id } = useLocalSearchParams<{ id: string }>();
  const shopId = Number(id);

  const [shop, setShop] = useState<BarberShop | null>(null);
  const [services, setServices] = useState<Service[]>([]);
  const [barbers, setBarbers] = useState<Barber[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useFocusEffect(
    useCallback(() => {
      let cancelled = false;
      (async () => {
        setLoading(true);
        setError(null);
        try {
          const [shopData, servicesData, barbersData] = await Promise.all([
            getBarberShop(shopId),
            listServices(shopId),
            listShopBarbers(shopId),
          ]);
          if (!cancelled) {
            setShop(shopData);
            setServices(servicesData.filter((s) => s.available));
            setBarbers(barbersData.filter((b) => b.available));
          }
        } catch (err) {
          if (!cancelled) {
            setError(err instanceof ApiError ? err.message : 'Não foi possível carregar a barbearia.');
          }
        } finally {
          if (!cancelled) {
            setLoading(false);
          }
        }
      })();
      return () => {
        cancelled = true;
      };
    }, [shopId]),
  );

  if (loading) {
    return (
      <SafeAreaView style={styles.safe}>
        <ActivityIndicator style={styles.loading} color={colors.black} />
      </SafeAreaView>
    );
  }

  if (error || !shop) {
    return (
      <SafeAreaView style={styles.safe} edges={['top']}>
        <View style={styles.topBar}>
          <Pressable onPress={() => router.back()} hitSlop={12}>
            <Ionicons name="chevron-back" size={24} color={colors.black} />
          </Pressable>
        </View>
        <View style={styles.content}>
          <Text style={styles.empty}>{error ?? 'Barbearia não encontrada.'}</Text>
        </View>
      </SafeAreaView>
    );
  }

  const bookable = services.length > 0 && barbers.length > 0;

  return (
    <SafeAreaView style={styles.safe} edges={['top']}>
      <View style={styles.topBar}>
        <Pressable onPress={() => router.back()} hitSlop={12}>
          <Ionicons name="chevron-back" size={24} color={colors.black} />
        </Pressable>
      </View>

      <ScrollView contentContainerStyle={styles.content}>
        <View style={styles.hero}>
          {shop.photoBase64 ? (
            <Avatar avatarBase64={shop.photoBase64} name={shop.name} size={52} />
          ) : (
            <View style={styles.heroCrest}>
              <Ionicons name="cut" size={22} color={colors.blue} />
            </View>
          )}
          <Text style={styles.shopName}>{shop.name}</Text>
          {shop.address ? (
            <Text style={styles.shopMeta}>
              <Ionicons name="location-outline" size={13} color={colors.textMuted} /> {shop.address}
            </Text>
          ) : null}
          {shop.phone ? (
            <Text style={styles.shopMeta}>
              <Ionicons name="call-outline" size={13} color={colors.textMuted} /> {shop.phone}
            </Text>
          ) : null}
          <View style={[styles.tag, { borderColor: shop.acceptingBarbers ? colors.success : colors.pillBorder }]}>
            <Text style={[styles.tagText, { color: shop.acceptingBarbers ? colors.success : colors.textMuted }]}>
              {shop.acceptingBarbers ? 'Aceitando novos barbeiros' : 'Equipe completa'}
            </Text>
          </View>
        </View>

        <SectionHeader title="Serviços" />
        {services.length === 0 ? (
          <Text style={styles.empty}>Nenhum serviço disponível.</Text>
        ) : (
          services.map((service) => (
            <Card key={service.id} style={styles.serviceRow}>
              <View style={styles.serviceInfo}>
                <Text style={styles.serviceName}>{service.name}</Text>
                {service.description ? (
                  <Text style={styles.serviceDesc} numberOfLines={2}>
                    {service.description}
                  </Text>
                ) : null}
                <Text style={styles.serviceMeta}>{formatDuration(service.durationMinutes)}</Text>
              </View>
              <Text style={styles.servicePrice}>{formatCurrency(service.price)}</Text>
            </Card>
          ))
        )}

        <SectionHeader title="Barbeiros" />
        {barbers.length === 0 ? (
          <Text style={styles.empty}>Nenhum barbeiro disponível.</Text>
        ) : (
          <View style={styles.barbersRow}>
            {barbers.map((barber) => (
              <View key={barber.id} style={styles.barberChip}>
                <Avatar name={barber.name} avatarBase64={barber.avatarBase64} size={56} />
                <Text style={styles.barberName} numberOfLines={1}>
                  {barber.name}
                </Text>
              </View>
            ))}
          </View>
        )}
      </ScrollView>

      <View style={styles.footer}>
        <Button
          title="Agendar Horário"
          disabled={!bookable}
          onPress={() =>
            router.push({ pathname: '/(app)/book/[shopId]', params: { shopId: String(shopId) } })
          }
        />
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
  topBar: {
    paddingHorizontal: spacing.lg,
    paddingTop: spacing.sm,
    ...centeredPage,
  },
  content: {
    padding: spacing.lg,
    paddingBottom: spacing.xxl,
    ...centeredPage,
  },
  hero: {
    alignItems: 'flex-start',
  },
  heroCrest: {
    width: 52,
    height: 52,
    borderRadius: 26,
    backgroundColor: colors.blueSoft,
    alignItems: 'center',
    justifyContent: 'center',
    marginBottom: spacing.sm,
  },
  shopName: {
    ...typography.h1,
    color: colors.black,
  },
  shopMeta: {
    fontSize: 14,
    color: colors.textMuted,
    marginTop: 4,
  },
  tag: {
    borderWidth: 1.5,
    borderRadius: radius.pill,
    paddingHorizontal: spacing.sm,
    paddingVertical: 4,
    marginTop: spacing.sm,
  },
  tagText: {
    ...typography.caption,
    textTransform: 'uppercase',
    letterSpacing: 0.5,
  },
  empty: {
    color: colors.textMuted,
    fontSize: 14,
  },
  serviceRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: spacing.sm,
  },
  serviceInfo: {
    flex: 1,
    marginRight: spacing.sm,
  },
  serviceName: {
    fontSize: 15,
    color: colors.black,
    fontWeight: '600',
  },
  serviceDesc: {
    fontSize: 12,
    color: colors.textMuted,
    marginTop: 2,
  },
  serviceMeta: {
    fontSize: 12,
    color: colors.textMuted,
    marginTop: 4,
  },
  servicePrice: {
    fontFamily: typography.h2.fontFamily,
    fontSize: 16,
    color: colors.black,
  },
  barbersRow: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: spacing.md,
  },
  barberChip: {
    alignItems: 'center',
    width: 72,
  },
  barberName: {
    fontSize: 12,
    color: colors.black,
    textAlign: 'center',
    marginTop: 4,
  },
  footer: {
    padding: spacing.lg,
    borderTopWidth: 1,
    borderTopColor: colors.line,
    backgroundColor: colors.surface,
    ...centeredPage,
  },
});
