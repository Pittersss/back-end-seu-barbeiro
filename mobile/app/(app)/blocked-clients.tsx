import { Ionicons } from '@expo/vector-icons';
import { router, useFocusEffect } from 'expo-router';
import { useCallback, useState } from 'react';
import { ActivityIndicator, Pressable, ScrollView, StyleSheet, Text, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';

import { Avatar } from '../../components/Avatar';
import { Card } from '../../components/Card';
import { ConfirmDialog } from '../../components/ConfirmDialog';
import { useAuth } from '../../context/AuthContext';
import { ApiError } from '../../lib/api';
import { listBlockedClients, unblockClient } from '../../lib/api/barbers';
import { formatDate } from '../../lib/format';
import type { BlockedClient } from '../../lib/types';
import { colors } from '../../theme/colors';
import { centeredPage } from '../../theme/layout';
import { spacing } from '../../theme/spacing';
import { typography } from '../../theme/typography';

export default function BlockedClientsScreen() {
  const { session } = useAuth();
  const barberId = session?.userId;

  const [items, setItems] = useState<BlockedClient[]>([]);
  const [loading, setLoading] = useState(true);
  const [pending, setPending] = useState<BlockedClient | null>(null);
  const [working, setWorking] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(async () => {
    if (!barberId) return;
    try {
      setItems(await listBlockedClients(barberId));
    } catch {
      // 401s handled by AuthContext.
    } finally {
      setLoading(false);
    }
  }, [barberId]);

  useFocusEffect(
    useCallback(() => {
      load();
    }, [load]),
  );

  async function handleUnblock() {
    if (!barberId || !pending) return;
    setWorking(true);
    try {
      await unblockClient(barberId, pending.clientId);
      setItems((prev) => prev.filter((b) => b.clientId !== pending.clientId));
      setPending(null);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Não foi possível desbloquear.');
    } finally {
      setWorking(false);
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
      <View style={styles.header}>
        <Pressable onPress={() => router.back()} hitSlop={12}>
          <Ionicons name="chevron-back" size={24} color={colors.black} />
        </Pressable>
        <Text style={styles.headerTitle}>Clientes bloqueados</Text>
        <View style={{ width: 24 }} />
      </View>

      <ScrollView contentContainerStyle={styles.content}>
        {error ? <Text style={styles.error}>{error}</Text> : null}

        {items.length === 0 ? (
          <View style={styles.empty}>
            <Ionicons name="people-outline" size={40} color={colors.textFaint} />
            <Text style={styles.emptyText}>
              Nenhum cliente bloqueado. Você pode bloquear um cliente a partir de um
              agendamento.
            </Text>
          </View>
        ) : (
          items.map((item) => (
            <Card key={item.id} style={styles.row} variant="flat">
              <Avatar name={item.clientName} size={40} />
              <View style={styles.info}>
                <Text style={styles.name}>{item.clientName}</Text>
                <Text style={styles.meta}>
                  {item.clientPhone ? `${item.clientPhone} · ` : ''}
                  desde {formatDate(item.createdAt)}
                </Text>
                {item.reason ? <Text style={styles.meta}>{item.reason}</Text> : null}
              </View>
              <Pressable onPress={() => setPending(item)} hitSlop={8}>
                <Text style={styles.action}>Desbloquear</Text>
              </Pressable>
            </Card>
          ))
        )}
      </ScrollView>

      <ConfirmDialog
        visible={pending !== null}
        title="Desbloquear cliente?"
        message={pending ? `${pending.clientName} poderá agendar com você novamente.` : undefined}
        confirmLabel="Desbloquear"
        loading={working}
        onConfirm={handleUnblock}
        onCancel={() => setPending(null)}
      />
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
    marginBottom: spacing.sm,
  },
  empty: {
    alignItems: 'center',
    gap: spacing.md,
    marginTop: spacing.xxl,
    paddingHorizontal: spacing.lg,
  },
  emptyText: {
    color: colors.textMuted,
    textAlign: 'center',
    fontSize: 14,
    lineHeight: 20,
  },
  row: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: spacing.md,
    marginBottom: spacing.sm,
  },
  info: {
    flex: 1,
  },
  name: {
    fontSize: 15,
    color: colors.black,
    fontWeight: '600',
  },
  meta: {
    fontSize: 12,
    color: colors.textMuted,
    marginTop: 2,
  },
  action: {
    color: colors.blue,
    fontSize: 13,
    fontWeight: '600',
  },
});
