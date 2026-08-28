import { Ionicons } from '@expo/vector-icons';
import { router, useFocusEffect, useLocalSearchParams } from 'expo-router';
import { useCallback, useState } from 'react';
import { ActivityIndicator, Pressable, ScrollView, StyleSheet, Text, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';

import { Button } from '../../../components/Button';
import { getBarberShop, listShopBarbers } from '../../../lib/api/barbershops';
import { listServices } from '../../../lib/api/services';
import { formatCurrency, formatDuration, initials } from '../../../lib/format';
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

  useFocusEffect(
    useCallback(() => {
      let cancelled = false;
      (async () => {
        setLoading(true);
        const [shopData, servicesData, barbersData] = await Promise.all([
          getBarberShop(shopId),
          listServices(shopId),
          listShopBarbers(shopId),
        ]);
        if (!cancelled) {
          setShop(shopData);
          setServices(servicesData.filter((s) => s.available));
          setBarbers(barbersData.filter((b) => b.available));
          setLoading(false);
        }
      })();
      return () => {
        cancelled = true;
      };
    }, [shopId]),
  );

  if (loading || !shop) {
    return (
      <SafeAreaView style={styles.safe}>
        <ActivityIndicator style={styles.loading} color={colors.black} />
      </SafeAreaView>
    );
  }

  return (
    <SafeAreaView style={styles.safe} edges={['top']}>
      <View style={styles.header}>
        <Pressable onPress={() => router.back()} hitSlop={12}>
          <Ionicons name="chevron-back" size={24} color={colors.black} />
        </Pressable>
      </View>

      <ScrollView contentContainerStyle={styles.content}>
        <Text style={styles.shopName}>{shop.name}</Text>
        {shop.address ? <Text style={styles.shopMeta}>{shop.address}</Text> : null}
        {shop.phone ? <Text style={styles.shopMeta}>{shop.phone}</Text> : null}

        <Text style={styles.sectionTitle}>Serviços</Text>
        {services.length === 0 ? (
          <Text style={styles.empty}>Nenhum serviço disponível.</Text>
        ) : (
          services.map((service) => (
            <View key={service.id} style={styles.serviceRow}>
              <View style={styles.serviceInfo}>
                <Text style={styles.serviceName}>{service.name}</Text>
                <Text style={styles.serviceMeta}>{formatDuration(service.durationMinutes)}</Text>
              </View>
              <Text style={styles.servicePrice}>{formatCurrency(service.price)}</Text>
            </View>
          ))
        )}

        <Text style={styles.sectionTitle}>Barbeiros</Text>
        {barbers.length === 0 ? (
          <Text style={styles.empty}>Nenhum barbeiro disponível.</Text>
        ) : (
          <View style={styles.barbersRow}>
            {barbers.map((barber) => (
              <View key={barber.id} style={styles.barberChip}>
                <View style={styles.barberAvatar}>
                  <Text style={styles.barberAvatarText}>{initials(barber.name)}</Text>
                </View>
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
          disabled={services.length === 0 || barbers.length === 0}
          onPress={() => router.push({ pathname: '/(app)/book/[shopId]', params: { shopId: String(shopId) } })}
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
  header: {
    paddingHorizontal: spacing.lg,
    paddingTop: spacing.sm,
    ...centeredPage,
  },
  content: {
    padding: spacing.lg,
    paddingBottom: spacing.xxl,
    ...centeredPage,
  },
  shopName: {
    ...typography.h1,
    color: colors.black,
  },
  shopMeta: {
    fontSize: 14,
    color: colors.textMuted,
    marginTop: 2,
  },
  sectionTitle: {
    ...typography.h2,
    color: colors.black,
    marginTop: spacing.xl,
    marginBottom: spacing.sm,
  },
  empty: {
    color: colors.textMuted,
    fontSize: 14,
  },
  serviceRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    paddingVertical: spacing.sm,
    borderBottomWidth: 1,
    borderBottomColor: colors.pillBorder,
  },
  serviceInfo: {
    flex: 1,
  },
  serviceName: {
    fontSize: 15,
    color: colors.black,
    fontWeight: '600',
  },
  serviceMeta: {
    fontSize: 12,
    color: colors.textMuted,
    marginTop: 2,
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
  barberAvatar: {
    width: 56,
    height: 56,
    borderRadius: 28,
    backgroundColor: colors.blue,
    alignItems: 'center',
    justifyContent: 'center',
    marginBottom: 4,
  },
  barberAvatarText: {
    color: colors.white,
    fontFamily: typography.h2.fontFamily,
    fontSize: 16,
  },
  barberName: {
    fontSize: 12,
    color: colors.black,
    textAlign: 'center',
  },
  footer: {
    padding: spacing.lg,
    borderTopWidth: 1,
    borderTopColor: colors.pillBorder,
    ...centeredPage,
  },
});
