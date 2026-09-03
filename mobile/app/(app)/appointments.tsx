import { Ionicons } from '@expo/vector-icons';
import { router, useFocusEffect } from 'expo-router';
import { useCallback, useMemo, useState } from 'react';
import { FlatList, Pressable, RefreshControl, StyleSheet, Text, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';

import { Avatar } from '../../components/Avatar';
import { StatusBadge } from '../../components/Badge';
import { Card } from '../../components/Card';
import { ConfirmDialog } from '../../components/ConfirmDialog';
import { useAuth } from '../../context/AuthContext';
import { ApiError } from '../../lib/api';
import { cancelAppointment, listAppointments, updateAppointmentStatus } from '../../lib/api/appointments';
import { blockClient, listBlockedClients } from '../../lib/api/barbers';
import { formatDate, formatTime } from '../../lib/format';
import type { Appointment } from '../../lib/types';
import { colors } from '../../theme/colors';
import { centeredPage } from '../../theme/layout';
import { radius, spacing } from '../../theme/spacing';
import { typography } from '../../theme/typography';

type Filter = 'upcoming' | 'history';
type PendingAction = { type: 'cancel' | 'block'; appointment: Appointment };

const OPEN_STATUSES: Appointment['status'][] = ['PENDING', 'CONFIRMED'];
const PAYMENT_LABELS: Record<string, string> = { PIX: 'Pix', CARD: 'Cartão', CASH: 'Dinheiro' };

export default function AppointmentsScreen() {
  const { session } = useAuth();
  const isBarber = session?.role === 'BARBER';

  const [appointments, setAppointments] = useState<Appointment[]>([]);
  const [blockedIds, setBlockedIds] = useState<Set<number>>(new Set());
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [actioningId, setActioningId] = useState<number | null>(null);
  const [filter, setFilter] = useState<Filter>('upcoming');
  const [pending, setPending] = useState<PendingAction | null>(null);
  const [working, setWorking] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(async () => {
    try {
      const data = await listAppointments();
      data.sort((a, b) => new Date(b.scheduledAt).getTime() - new Date(a.scheduledAt).getTime());
      setAppointments(data);
      if (isBarber && session) {
        try {
          const blocked = await listBlockedClients(session.userId);
          setBlockedIds(new Set(blocked.map((b) => b.clientId)));
        } catch {
          // non-fatal
        }
      }
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  }, [isBarber, session]);

  useFocusEffect(
    useCallback(() => {
      load();
    }, [load]),
  );

  async function handleAdvanceStatus(appointment: Appointment, status: Appointment['status']) {
    setActioningId(appointment.id);
    try {
      await updateAppointmentStatus(appointment.id, status);
      await load();
    } finally {
      setActioningId(null);
    }
  }

  async function runPendingAction() {
    if (!pending || !session) return;
    setError(null);
    setWorking(true);
    try {
      if (pending.type === 'cancel') {
        await cancelAppointment(pending.appointment.id);
      } else {
        await blockClient(session.userId, { clientId: pending.appointment.clientId });
        setBlockedIds((prev) => new Set(prev).add(pending.appointment.clientId));
      }
      setPending(null);
      await load();
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Não foi possível concluir a ação.');
    } finally {
      setWorking(false);
    }
  }

  const visible = useMemo(() => {
    const open = filter === 'upcoming';
    return appointments.filter((a) => OPEN_STATUSES.includes(a.status) === open);
  }, [appointments, filter]);

  return (
    <SafeAreaView style={styles.safe} edges={['top']}>
      <View style={styles.header}>
        <Text style={styles.title}>Meus agendamentos</Text>
        <View style={styles.segment}>
          {(['upcoming', 'history'] as Filter[]).map((key) => (
            <Pressable
              key={key}
              onPress={() => setFilter(key)}
              style={[styles.segmentItem, filter === key && styles.segmentItemActive]}
            >
              <Text style={[styles.segmentText, filter === key && styles.segmentTextActive]}>
                {key === 'upcoming' ? 'Próximos' : 'Histórico'}
              </Text>
            </Pressable>
          ))}
        </View>
        {error ? <Text style={styles.error}>{error}</Text> : null}
      </View>

      <FlatList
        data={visible}
        keyExtractor={(item) => String(item.id)}
        contentContainerStyle={styles.list}
        refreshControl={
          <RefreshControl
            refreshing={refreshing}
            onRefresh={() => {
              setRefreshing(true);
              load();
            }}
          />
        }
        ListEmptyComponent={
          !loading ? (
            <Text style={styles.empty}>
              {filter === 'upcoming'
                ? 'Nenhum agendamento em aberto.'
                : 'Nenhum agendamento no histórico.'}
            </Text>
          ) : null
        }
        renderItem={({ item }) => {
          const busy = actioningId === item.id;
          const canCancel = item.status === 'PENDING' || item.status === 'CONFIRMED';
          const counterpart = isBarber ? item.clientName : item.barberName;
          const counterpartAvatar = isBarber ? item.clientAvatarBase64 : item.barberAvatarBase64;
          const canBlock = isBarber && !blockedIds.has(item.clientId);
          return (
            <Card style={styles.card}>
              <View style={styles.cardHeader}>
                <Text style={styles.serviceName}>{item.serviceName}</Text>
                <StatusBadge status={item.status} />
              </View>

              <View style={styles.metaRow}>
                <Ionicons name="calendar-outline" size={14} color={colors.textMuted} />
                <Text style={styles.meta}>
                  {formatDate(item.scheduledAt)} · {formatTime(item.scheduledAt)}
                </Text>
              </View>
              <View style={styles.metaRow}>
                <Avatar name={counterpart} avatarBase64={counterpartAvatar} size={22} />
                <Text style={styles.meta}>
                  {isBarber ? 'Cliente' : 'Barbeiro'}: {counterpart}
                </Text>
              </View>
              <View style={styles.metaRow}>
                <Ionicons name="wallet-outline" size={14} color={colors.textMuted} />
                <Text style={styles.meta}>
                  Pagamento: {PAYMENT_LABELS[item.paymentMethod] ?? item.paymentMethod}
                </Text>
              </View>

              <View style={styles.actionsRow}>
                {item.paymentMethod === 'PIX' && item.status !== 'CANCELLED' ? (
                  <Pressable
                    onPress={() =>
                      router.push({
                        pathname: '/(app)/appointment/[id]/pix',
                        params: { id: String(item.id) },
                      })
                    }
                  >
                    <Text style={styles.linkAction}>Ver Pix</Text>
                  </Pressable>
                ) : null}

                {isBarber && item.status === 'PENDING' ? (
                  <Pressable disabled={busy} onPress={() => handleAdvanceStatus(item, 'CONFIRMED')}>
                    <Text style={styles.linkAction}>Confirmar</Text>
                  </Pressable>
                ) : null}
                {isBarber && item.status === 'CONFIRMED' ? (
                  <Pressable disabled={busy} onPress={() => handleAdvanceStatus(item, 'COMPLETED')}>
                    <Text style={styles.linkAction}>Concluir</Text>
                  </Pressable>
                ) : null}

                {canCancel ? (
                  <Pressable onPress={() => setPending({ type: 'cancel', appointment: item })}>
                    <Text style={styles.dangerAction}>Cancelar</Text>
                  </Pressable>
                ) : null}
                {canBlock ? (
                  <Pressable onPress={() => setPending({ type: 'block', appointment: item })}>
                    <Text style={styles.dangerAction}>Bloquear cliente</Text>
                  </Pressable>
                ) : null}
              </View>
            </Card>
          );
        }}
      />

      <ConfirmDialog
        visible={pending !== null}
        title={pending?.type === 'block' ? 'Bloquear cliente?' : 'Cancelar agendamento?'}
        message={
          pending?.type === 'block'
            ? `${pending.appointment.clientName} não poderá mais agendar com você.`
            : 'Esta ação não pode ser desfeita.'
        }
        confirmLabel={pending?.type === 'block' ? 'Bloquear' : 'Cancelar agendamento'}
        cancelLabel="Voltar"
        destructive
        loading={working}
        onConfirm={runPendingAction}
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
  header: {
    paddingHorizontal: spacing.lg,
    paddingTop: spacing.md,
    paddingBottom: spacing.sm,
    ...centeredPage,
  },
  title: {
    ...typography.h1,
    color: colors.black,
  },
  segment: {
    flexDirection: 'row',
    backgroundColor: colors.pill,
    borderRadius: radius.pill,
    padding: 3,
    marginTop: spacing.md,
  },
  segmentItem: {
    flex: 1,
    alignItems: 'center',
    paddingVertical: spacing.sm,
    borderRadius: radius.pill,
  },
  segmentItemActive: {
    backgroundColor: colors.surface,
  },
  segmentText: {
    ...typography.label,
    color: colors.textMuted,
  },
  segmentTextActive: {
    color: colors.black,
  },
  error: {
    color: colors.red,
    marginTop: spacing.sm,
    fontSize: 13,
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
    padding: spacing.md,
  },
  cardHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'flex-start',
    marginBottom: spacing.sm,
  },
  serviceName: {
    fontFamily: typography.h2.fontFamily,
    fontSize: 16,
    color: colors.black,
    flex: 1,
    marginRight: spacing.sm,
  },
  metaRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
    marginTop: 4,
  },
  meta: {
    fontSize: 13,
    color: colors.textMuted,
  },
  actionsRow: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: spacing.md,
    marginTop: spacing.md,
  },
  linkAction: {
    color: colors.blue,
    fontSize: 13,
    fontWeight: '600',
  },
  dangerAction: {
    color: colors.red,
    fontSize: 13,
    fontWeight: '600',
  },
});
