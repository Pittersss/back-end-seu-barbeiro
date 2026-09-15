import { Ionicons } from '@expo/vector-icons';
import { useFocusEffect } from 'expo-router';
import { useCallback, useState } from 'react';
import {
  ActivityIndicator,
  RefreshControl,
  ScrollView,
  Text,
  View,
} from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';

import { Avatar } from './Avatar';
import { Button } from './Button';
import { Card } from './Card';
import { ConfirmDialog } from './ConfirmDialog';
import {
  decideBarberShopRequest,
  decideSubscriptionPayment,
  deleteBarberShop,
  deleteClient,
  listBarberShopRequests,
  listClients,
  listPendingSubscriptionPayments,
} from '../lib/api/admin';
import { listBarberShops } from '../lib/api/barbershops';
import { useAuth } from '../context/AuthContext';
import { formatCurrency, formatDateTime } from '../lib/format';
import type {
  BarberShop,
  BarberShopRequestResponse,
  SubscriptionPaymentResponse,
  UserProfile,
} from '../lib/types';
import { centeredPage } from '../theme/layout';
import { spacing } from '../theme/spacing';
import { useThemeColors } from '../theme/ThemeContext';
import { typography } from '../theme/typography';
import { useThemedStyles } from '../theme/useThemedStyles';

type PendingDelete = { type: 'shop' | 'client'; id: number; label: string };

export function AdminHome() {
  const colors = useThemeColors();
  const styles = useThemedStyles((colors) => ({
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
    sectionTitle: {
      ...typography.label,
      color: colors.textMuted,
      marginBottom: spacing.sm,
      marginTop: spacing.md,
    },
    card: {
      marginBottom: spacing.md,
    },
    emptyText: {
      fontSize: 14,
      color: colors.textMuted,
    },
    itemTitle: {
      fontFamily: typography.h2.fontFamily,
      fontSize: 16,
      color: colors.black,
    },
    itemSubtitle: {
      fontSize: 14,
      color: colors.textMuted,
      marginTop: 2,
    },
    itemDetail: {
      fontSize: 13,
      color: colors.black,
      marginTop: 2,
    },
    itemDate: {
      fontSize: 12,
      color: colors.textFaint,
      marginTop: spacing.xs,
    },
    statusRow: {
      flexDirection: 'row',
      alignItems: 'center',
      gap: spacing.xs,
    },
    actionRow: {
      flexDirection: 'row',
      gap: spacing.sm,
      marginTop: spacing.md,
    },
    actionButton: {
      flex: 1,
    },
    deleteButton: {
      borderColor: colors.danger,
    },
    error: {
      color: colors.red,
      textAlign: 'center',
      marginTop: spacing.sm,
    },
  }));
  const { session } = useAuth();
  const [shopRequests, setShopRequests] = useState<BarberShopRequestResponse[]>([]);
  const [payments, setPayments] = useState<SubscriptionPaymentResponse[]>([]);
  const [shops, setShops] = useState<BarberShop[]>([]);
  const [clients, setClients] = useState<UserProfile[]>([]);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [busyId, setBusyId] = useState<string | null>(null);
  const [pendingDelete, setPendingDelete] = useState<PendingDelete | null>(null);
  const [deleting, setDeleting] = useState(false);
  const [deleteError, setDeleteError] = useState<string | null>(null);

  const load = useCallback(async () => {
    try {
      const [requests, pendingPayments, allShops, allClients] = await Promise.all([
        listBarberShopRequests(),
        listPendingSubscriptionPayments(),
        listBarberShops(),
        listClients(),
      ]);
      setShopRequests(requests);
      setPayments(pendingPayments);
      setShops(allShops);
      setClients(allClients);
    } catch {
      // A 401 here means the session was stale/invalid; AuthContext's
      // unauthorized handler already clears it and redirects to login.
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

  async function handleShopDecision(id: number, approved: boolean) {
    setBusyId(`shop-${id}`);
    try {
      await decideBarberShopRequest(id, approved);
      setShopRequests((prev) => prev.filter((r) => r.id !== id));
    } finally {
      setBusyId(null);
    }
  }

  async function handlePaymentDecision(id: number, approved: boolean) {
    setBusyId(`payment-${id}`);
    try {
      await decideSubscriptionPayment(id, approved);
      setPayments((prev) => prev.filter((p) => p.id !== id));
    } finally {
      setBusyId(null);
    }
  }

  async function handleConfirmDelete() {
    if (!pendingDelete) return;
    setDeleting(true);
    setDeleteError(null);
    try {
      if (pendingDelete.type === 'shop') {
        await deleteBarberShop(pendingDelete.id);
        setShops((prev) => prev.filter((s) => s.id !== pendingDelete.id));
      } else {
        await deleteClient(pendingDelete.id);
        setClients((prev) => prev.filter((c) => c.id !== pendingDelete.id));
      }
      setPendingDelete(null);
    } catch {
      setDeleteError('Não foi possível excluir. Tente novamente.');
    } finally {
      setDeleting(false);
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
        refreshControl={
          <RefreshControl
            refreshing={refreshing}
            onRefresh={() => {
              setRefreshing(true);
              load();
            }}
          />
        }
      >
        <View style={styles.greetingRow}>
          <Avatar name={session?.name} avatarBase64={session?.avatarBase64} size={40} tone="black" />
          <View>
            <Text style={styles.greeting}>Olá, {session?.name?.split(' ')[0]}</Text>
            <Text style={styles.title}>Painel do administrador</Text>
          </View>
        </View>

        <Text style={styles.sectionTitle}>Solicitações de barbearia</Text>
        {shopRequests.length === 0 ? (
          <Card style={styles.card}>
            <Text style={styles.emptyText}>Nenhuma solicitação pendente.</Text>
          </Card>
        ) : (
          shopRequests.map((r) => (
            <Card key={r.id} style={styles.card}>
              <Text style={styles.itemTitle}>{r.shopName}</Text>
              <Text style={styles.itemSubtitle}>Solicitado por {r.requesterName}</Text>
              {r.shopAddress ? <Text style={styles.itemDetail}>{r.shopAddress}</Text> : null}
              {r.shopPhone ? <Text style={styles.itemDetail}>{r.shopPhone}</Text> : null}
              <Text style={styles.itemDate}>{formatDateTime(r.createdAt)}</Text>
              <View style={styles.actionRow}>
                <Button
                  title="Rejeitar"
                  variant="outline"
                  size="sm"
                  onPress={() => handleShopDecision(r.id, false)}
                  loading={busyId === `shop-${r.id}`}
                  style={styles.actionButton}
                />
                <Button
                  title="Aprovar"
                  size="sm"
                  onPress={() => handleShopDecision(r.id, true)}
                  loading={busyId === `shop-${r.id}`}
                  style={styles.actionButton}
                />
              </View>
            </Card>
          ))
        )}

        <Text style={styles.sectionTitle}>Pagamentos de assinatura</Text>
        {payments.length === 0 ? (
          <Card style={styles.card}>
            <Text style={styles.emptyText}>Nenhum pagamento pendente.</Text>
          </Card>
        ) : (
          payments.map((p) => (
            <Card key={p.id} style={styles.card}>
              <View style={styles.statusRow}>
                <Ionicons name="time-outline" size={18} color={colors.warning} />
                <Text style={styles.itemTitle}>{p.barberName}</Text>
              </View>
              <Text style={styles.itemSubtitle}>{formatCurrency(p.amount)}</Text>
              {p.txId ? <Text style={styles.itemDetail}>TxID: {p.txId}</Text> : null}
              <Text style={styles.itemDate}>{formatDateTime(p.createdAt)}</Text>
              <View style={styles.actionRow}>
                <Button
                  title="Rejeitar"
                  variant="outline"
                  size="sm"
                  onPress={() => handlePaymentDecision(p.id, false)}
                  loading={busyId === `payment-${p.id}`}
                  style={styles.actionButton}
                />
                <Button
                  title="Confirmar"
                  size="sm"
                  onPress={() => handlePaymentDecision(p.id, true)}
                  loading={busyId === `payment-${p.id}`}
                  style={styles.actionButton}
                />
              </View>
            </Card>
          ))
        )}

        {deleteError ? <Text style={styles.error}>{deleteError}</Text> : null}

        <Text style={styles.sectionTitle}>Barbearias</Text>
        {shops.length === 0 ? (
          <Card style={styles.card}>
            <Text style={styles.emptyText}>Nenhuma barbearia cadastrada.</Text>
          </Card>
        ) : (
          shops.map((s) => (
            <Card key={s.id} style={styles.card}>
              <Text style={styles.itemTitle}>{s.name}</Text>
              {s.address ? <Text style={styles.itemDetail}>{s.address}</Text> : null}
              <View style={styles.actionRow}>
                <Button
                  title="Excluir"
                  variant="outline"
                  size="sm"
                  onPress={() => setPendingDelete({ type: 'shop', id: s.id, label: s.name })}
                  style={[styles.actionButton, styles.deleteButton]}
                />
              </View>
            </Card>
          ))
        )}

        <Text style={styles.sectionTitle}>Clientes</Text>
        {clients.length === 0 ? (
          <Card style={styles.card}>
            <Text style={styles.emptyText}>Nenhum cliente cadastrado.</Text>
          </Card>
        ) : (
          clients.map((c) => (
            <Card key={c.id} style={styles.card}>
              <View style={styles.statusRow}>
                <Avatar name={c.name} avatarBase64={c.avatarBase64} size={32} />
                <View>
                  <Text style={styles.itemTitle}>{c.name}</Text>
                  <Text style={styles.itemSubtitle}>{c.email}</Text>
                </View>
              </View>
              <View style={styles.actionRow}>
                <Button
                  title="Excluir"
                  variant="outline"
                  size="sm"
                  onPress={() => setPendingDelete({ type: 'client', id: c.id, label: c.name })}
                  style={[styles.actionButton, styles.deleteButton]}
                />
              </View>
            </Card>
          ))
        )}
      </ScrollView>

      <ConfirmDialog
        visible={pendingDelete !== null}
        title={pendingDelete?.type === 'shop' ? 'Excluir barbearia?' : 'Excluir cliente?'}
        message={
          pendingDelete?.type === 'shop'
            ? `"${pendingDelete.label}" será removida permanentemente. O dono não poderá criar outra barbearia.`
            : `"${pendingDelete?.label}" e todo o seu histórico de agendamentos serão removidos permanentemente.`
        }
        confirmLabel="Excluir"
        cancelLabel="Voltar"
        destructive
        loading={deleting}
        onConfirm={handleConfirmDelete}
        onCancel={() => setPendingDelete(null)}
      />
    </SafeAreaView>
  );
}
