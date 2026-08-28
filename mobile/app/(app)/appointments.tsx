import { router, useFocusEffect } from 'expo-router';
import { useCallback, useState } from 'react';
import { FlatList, Pressable, RefreshControl, StyleSheet, Text, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';

import { StatusBadge } from '../../components/Badge';
import { useAuth } from '../../context/AuthContext';
import { cancelAppointment, listAppointments, updateAppointmentStatus } from '../../lib/api/appointments';
import { formatCurrency, formatDateTime } from '../../lib/format';
import type { Appointment } from '../../lib/types';
import { colors } from '../../theme/colors';
import { centeredPage } from '../../theme/layout';
import { radius, spacing } from '../../theme/spacing';
import { typography } from '../../theme/typography';

export default function AppointmentsScreen() {
  const { session } = useAuth();
  const [appointments, setAppointments] = useState<Appointment[]>([]);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [actioningId, setActioningId] = useState<number | null>(null);

  const load = useCallback(async () => {
    try {
      const data = await listAppointments();
      data.sort((a, b) => new Date(b.scheduledAt).getTime() - new Date(a.scheduledAt).getTime());
      setAppointments(data);
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

  async function handleCancel(appointment: Appointment) {
    setActioningId(appointment.id);
    try {
      await cancelAppointment(appointment.id);
      await load();
    } finally {
      setActioningId(null);
    }
  }

  async function handleAdvanceStatus(appointment: Appointment, status: Appointment['status']) {
    setActioningId(appointment.id);
    try {
      await updateAppointmentStatus(appointment.id, status);
      await load();
    } finally {
      setActioningId(null);
    }
  }

  const isBarber = session?.role === 'BARBER';

  return (
    <SafeAreaView style={styles.safe} edges={['top']}>
      <View style={styles.header}>
        <Text style={styles.title}>Meus Agendamentos</Text>
      </View>

      <FlatList
        data={appointments}
        keyExtractor={(item) => String(item.id)}
        contentContainerStyle={styles.list}
        refreshControl={<RefreshControl refreshing={refreshing} onRefresh={() => { setRefreshing(true); load(); }} />}
        ListEmptyComponent={!loading ? <Text style={styles.empty}>Você ainda não tem agendamentos.</Text> : null}
        renderItem={({ item }) => {
          const busy = actioningId === item.id;
          const canCancel = item.status === 'PENDING' || item.status === 'CONFIRMED';
          return (
            <View style={styles.card}>
              <View style={styles.cardHeader}>
                <Text style={styles.serviceName}>{item.serviceName}</Text>
                <StatusBadge status={item.status} />
              </View>
              <Text style={styles.meta}>{formatDateTime(item.scheduledAt)}</Text>
              <Text style={styles.meta}>
                {isBarber ? `Cliente: ${item.clientName}` : `Barbeiro: ${item.barberName}`}
              </Text>
              <Text style={styles.meta}>Pagamento: {item.paymentMethod}</Text>

              <View style={styles.actionsRow}>
                {item.paymentMethod === 'PIX' && item.status !== 'CANCELLED' ? (
                  <Pressable
                    onPress={() => router.push({ pathname: '/(app)/appointment/[id]/pix', params: { id: String(item.id) } })}
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
                  <Pressable disabled={busy} onPress={() => handleCancel(item)}>
                    <Text style={styles.dangerAction}>Cancelar</Text>
                  </Pressable>
                ) : null}
              </View>
            </View>
          );
        }}
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
    backgroundColor: colors.white,
    borderRadius: radius.card,
    borderWidth: 1,
    borderColor: colors.pillBorder,
    padding: spacing.md,
  },
  cardHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'flex-start',
    marginBottom: spacing.xs,
  },
  serviceName: {
    fontFamily: typography.h2.fontFamily,
    fontSize: 16,
    color: colors.black,
    flex: 1,
    marginRight: spacing.sm,
  },
  meta: {
    fontSize: 13,
    color: colors.textMuted,
    marginTop: 2,
  },
  actionsRow: {
    flexDirection: 'row',
    gap: spacing.md,
    marginTop: spacing.sm,
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
