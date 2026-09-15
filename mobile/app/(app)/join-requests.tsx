import { Ionicons } from '@expo/vector-icons';
import { router, useFocusEffect } from 'expo-router';
import { useCallback, useState } from 'react';
import { ActivityIndicator, Pressable, ScrollView, Text, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';

import { Button } from '../../components/Button';
import { Card } from '../../components/Card';
import { useAuth } from '../../context/AuthContext';
import { ApiError } from '../../lib/api';
import { getBarber } from '../../lib/api/barbers';
import { decideJoinRequest, listJoinRequests } from '../../lib/api/join-requests';
import { formatDateTime } from '../../lib/format';
import type { JoinRequestResponse } from '../../lib/types';
import { centeredPage } from '../../theme/layout';
import { spacing } from '../../theme/spacing';
import { useThemeColors } from '../../theme/ThemeContext';
import { typography } from '../../theme/typography';
import { useThemedStyles } from '../../theme/useThemedStyles';

export default function JoinRequestsScreen() {
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
    content: {
      padding: spacing.lg,
      paddingBottom: spacing.xxl,
      ...centeredPage,
    },
    error: {
      color: colors.red,
      textAlign: 'center',
      marginBottom: spacing.md,
    },
    empty: {
      textAlign: 'center',
      color: colors.textMuted,
      marginTop: spacing.xl,
    },
    card: {
      marginBottom: spacing.md,
    },
    itemTitle: {
      fontFamily: typography.h2.fontFamily,
      fontSize: 16,
      color: colors.black,
    },
    itemDetail: {
      fontSize: 13,
      color: colors.black,
      marginTop: 4,
    },
    itemDate: {
      fontSize: 12,
      color: colors.textFaint,
      marginTop: spacing.xs,
    },
    actionRow: {
      flexDirection: 'row',
      gap: spacing.sm,
      marginTop: spacing.md,
    },
    actionButton: {
      flex: 1,
    },
  }));
  const { session } = useAuth();
  const [shopId, setShopId] = useState<number | null>(null);
  const [requests, setRequests] = useState<JoinRequestResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [busyId, setBusyId] = useState<number | null>(null);

  const load = useCallback(async () => {
    if (!session) return;
    setError(null);
    try {
      const barber = await getBarber(session.userId);
      if (!barber.barberShopId) {
        setRequests([]);
        return;
      }
      setShopId(barber.barberShopId);
      setRequests(await listJoinRequests(barber.barberShopId));
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Não foi possível carregar as solicitações.');
    } finally {
      setLoading(false);
    }
  }, [session]);

  useFocusEffect(
    useCallback(() => {
      load();
    }, [load]),
  );

  async function handleDecision(requestId: number, accepted: boolean) {
    if (!shopId) return;
    setBusyId(requestId);
    try {
      await decideJoinRequest(shopId, requestId, accepted);
      setRequests((prev) => prev.filter((r) => r.id !== requestId));
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Não foi possível processar a solicitação.');
    } finally {
      setBusyId(null);
    }
  }

  return (
    <SafeAreaView style={styles.safe} edges={['top']}>
      <View style={styles.header}>
        <Pressable onPress={() => router.back()} hitSlop={12}>
          <Ionicons name="chevron-back" size={24} color={colors.black} />
        </Pressable>
        <Text style={styles.headerTitle}>Solicitações de entrada</Text>
        <View style={{ width: 24 }} />
      </View>

      {loading ? (
        <ActivityIndicator style={styles.loading} color={colors.black} />
      ) : (
        <ScrollView contentContainerStyle={styles.content}>
          {error ? <Text style={styles.error}>{error}</Text> : null}

          {requests.length === 0 ? (
            <Text style={styles.empty}>Nenhuma solicitação pendente.</Text>
          ) : (
            requests.map((r) => (
              <Card key={r.id} style={styles.card}>
                <Text style={styles.itemTitle}>{r.barberName}</Text>
                {r.message ? <Text style={styles.itemDetail}>{r.message}</Text> : null}
                <Text style={styles.itemDate}>{formatDateTime(r.createdAt)}</Text>
                <View style={styles.actionRow}>
                  <Button
                    title="Rejeitar"
                    variant="outline"
                    size="sm"
                    onPress={() => handleDecision(r.id, false)}
                    loading={busyId === r.id}
                    style={styles.actionButton}
                  />
                  <Button
                    title="Aceitar"
                    size="sm"
                    onPress={() => handleDecision(r.id, true)}
                    loading={busyId === r.id}
                    style={styles.actionButton}
                  />
                </View>
              </Card>
            ))
          )}
        </ScrollView>
      )}
    </SafeAreaView>
  );
}
