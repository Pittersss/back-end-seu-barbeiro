import { Ionicons } from '@expo/vector-icons';
import { router, useFocusEffect } from 'expo-router';
import { useCallback, useState } from 'react';
import {
  ActivityIndicator,
  FlatList,
  Pressable,
  RefreshControl,
  Text,
  View,
} from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';

import { Avatar } from '../../components/Avatar';
import { Card } from '../../components/Card';
import { listBarberShops } from '../../lib/api/barbershops';
import type { BarberShop } from '../../lib/types';
import { centeredPage } from '../../theme/layout';
import { spacing } from '../../theme/spacing';
import { useThemeColors } from '../../theme/ThemeContext';
import { typography } from '../../theme/typography';
import { useThemedStyles } from '../../theme/useThemedStyles';

export default function JoinShopScreen() {
  const colors = useThemeColors();
  const styles = useThemedStyles((colors) => ({
    safe: {
      flex: 1,
      backgroundColor: colors.white,
    },
    loading: {
      flex: 1,
    },
    header: {
      flexDirection: 'row',
      alignItems: 'center',
      justifyContent: 'space-between',
      paddingHorizontal: spacing.lg,
      paddingTop: spacing.sm,
      ...centeredPage,
    },
    headerTitle: {
      ...typography.h2,
      color: colors.black,
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
    },
    cardPressed: {
      opacity: 0.7,
    },
    cardIcon: {
      width: 44,
      height: 44,
      borderRadius: 22,
      backgroundColor: colors.blueSoft,
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
  }));
  const [shops, setShops] = useState<BarberShop[]>([]);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);

  const load = useCallback(async () => {
    try {
      const all = await listBarberShops();
      setShops(all.filter((s) => s.acceptingBarbers));
    } catch {
      // Transient failures are fine — pull-to-refresh retries.
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
        <Pressable onPress={() => router.back()} hitSlop={12}>
          <Ionicons name="chevron-back" size={24} color={colors.black} />
        </Pressable>
        <Text style={styles.headerTitle}>Barbearias com vagas</Text>
        <View style={{ width: 24 }} />
      </View>

      {loading ? (
        <ActivityIndicator style={styles.loading} color={colors.black} />
      ) : (
        <FlatList
          data={shops}
          keyExtractor={(item) => String(item.id)}
          contentContainerStyle={styles.list}
          refreshControl={
            <RefreshControl refreshing={refreshing} onRefresh={() => { setRefreshing(true); load(); }} />
          }
          ListEmptyComponent={
            <Text style={styles.empty}>Nenhuma barbearia com vagas abertas no momento.</Text>
          }
          renderItem={({ item }) => (
            <Pressable
              style={({ pressed }) => pressed && styles.cardPressed}
              onPress={() => router.push({ pathname: '/(app)/shop/[id]', params: { id: String(item.id) } })}
            >
              <Card style={styles.card}>
                {item.photoBase64 ? (
                  <Avatar avatarBase64={item.photoBase64} name={item.name} size={44} style={styles.cardIcon} />
                ) : (
                  <View style={styles.cardIcon}>
                    <Ionicons name="cut" size={20} color={colors.blue} />
                  </View>
                )}
                <View style={styles.cardBody}>
                  <Text style={styles.cardTitle}>{item.name}</Text>
                  {item.address ? <Text style={styles.cardSubtitle}>{item.address}</Text> : null}
                </View>
                <Ionicons name="chevron-forward" size={20} color={colors.textFaint} />
              </Card>
            </Pressable>
          )}
        />
      )}
    </SafeAreaView>
  );
}
