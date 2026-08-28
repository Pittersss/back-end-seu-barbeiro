import { Ionicons } from '@expo/vector-icons';
import { router, useFocusEffect } from 'expo-router';
import { useCallback, useState } from 'react';
import { FlatList, Pressable, RefreshControl, StyleSheet, Text, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';

import { useAuth } from '../../context/AuthContext';
import { listBarberShops } from '../../lib/api/barbershops';
import type { BarberShop } from '../../lib/types';
import { colors } from '../../theme/colors';
import { centeredPage } from '../../theme/layout';
import { radius, spacing } from '../../theme/spacing';
import { typography } from '../../theme/typography';

export function ClientHome() {
  const { session } = useAuth();
  const [shops, setShops] = useState<BarberShop[]>([]);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);

  const load = useCallback(async () => {
    try {
      setShops(await listBarberShops());
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  }, []);

  useFocusEffect(
    useCallback(() => {
      load();
    }, [load]),
  );

  return (
    <SafeAreaView style={styles.safe} edges={['top']}>
      <View style={styles.header}>
        <Text style={styles.greeting}>Olá, {session?.name?.split(' ')[0]}</Text>
        <Text style={styles.title}>Escolha sua barbearia</Text>
      </View>

      <FlatList
        data={shops}
        keyExtractor={(item) => String(item.id)}
        contentContainerStyle={styles.list}
        refreshControl={<RefreshControl refreshing={refreshing} onRefresh={() => { setRefreshing(true); load(); }} />}
        ListEmptyComponent={
          !loading ? (
            <Text style={styles.empty}>Nenhuma barbearia disponível no momento.</Text>
          ) : null
        }
        renderItem={({ item }) => (
          <Pressable
            style={styles.card}
            onPress={() => router.push({ pathname: '/(app)/shop/[id]', params: { id: String(item.id) } })}
          >
            <View style={styles.cardIcon}>
              <Ionicons name="cut" size={20} color={colors.white} />
            </View>
            <View style={styles.cardBody}>
              <Text style={styles.cardTitle}>{item.name}</Text>
              {item.address ? <Text style={styles.cardSubtitle}>{item.address}</Text> : null}
              <Text style={[styles.cardStatus, { color: item.acceptingBarbers ? colors.success : colors.textMuted }]}>
                {item.acceptingBarbers ? 'Aceitando novos barbeiros' : 'Equipe completa'}
              </Text>
            </View>
            <Ionicons name="chevron-forward" size={20} color={colors.textFaint} />
          </Pressable>
        )}
      />
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  safe: {
    flex: 1,
    backgroundColor: colors.white,
  },
  header: {
    paddingHorizontal: spacing.lg,
    paddingTop: spacing.md,
    paddingBottom: spacing.sm,
    ...centeredPage,
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
  list: {
    padding: spacing.lg,
    gap: spacing.md,
    ...centeredPage,
  },
  empty: {
    textAlign: 'center',
    color: colors.textMuted,
    marginTop: spacing.xl,
  },
  card: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: colors.white,
    borderRadius: radius.card,
    borderWidth: 1,
    borderColor: colors.pillBorder,
    padding: spacing.md,
  },
  cardIcon: {
    width: 44,
    height: 44,
    borderRadius: 22,
    backgroundColor: colors.black,
    alignItems: 'center',
    justifyContent: 'center',
    marginRight: spacing.md,
  },
  cardBody: {
    flex: 1,
  },
  cardTitle: {
    fontFamily: typography.h2.fontFamily,
    fontSize: 16,
    color: colors.black,
  },
  cardSubtitle: {
    fontSize: 13,
    color: colors.textMuted,
    marginTop: 2,
  },
  cardStatus: {
    fontSize: 12,
    marginTop: 4,
  },
});
