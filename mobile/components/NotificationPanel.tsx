import { Ionicons } from '@expo/vector-icons';
import { router } from 'expo-router';
import { useState } from 'react';
import { FlatList, Modal, Pressable, Text, View } from 'react-native';

import { useNotifications } from '../context/NotificationContext';
import { formatDateTime } from '../lib/format';
import type { NotificationItem, NotificationType } from '../lib/types';
import { radius, spacing } from '../theme/spacing';
import { useThemeColors } from '../theme/ThemeContext';
import { typography } from '../theme/typography';
import { useThemedStyles } from '../theme/useThemedStyles';

interface NotificationPanelProps {
  visible: boolean;
  onClose: () => void;
}

// Where tapping each notification type should take the user — the screen that
// actually handles that situation, not just a generic inbox.
const NOTIFICATION_ROUTES: Partial<Record<NotificationType, string>> = {
  APPOINTMENT_REQUESTED: '/(app)/appointments',
  APPOINTMENT_CONFIRMED: '/(app)/appointments',
  APPOINTMENT_CANCELLED: '/(app)/appointments',
  BARBERSHOP_REQUEST: '/(app)/home',
  BARBERSHOP_REQUEST_DECIDED: '/(app)/home',
  SUBSCRIPTION_PAYMENT_PENDING: '/(app)/home',
  SUBSCRIPTION_PAYMENT_DECIDED: '/(app)/subscription',
  JOIN_REQUEST: '/(app)/join-requests',
  JOIN_REQUEST_DECIDED: '/(app)/home',
};

export function NotificationPanel({ visible, onClose }: NotificationPanelProps) {
  const colors = useThemeColors();
  const styles = useThemedStyles((colors) => ({
    backdrop: {
      flex: 1,
      backgroundColor: colors.overlay,
      alignItems: 'center',
      justifyContent: 'center',
      padding: spacing.lg,
    },
    panel: {
      width: '100%',
      maxWidth: 400,
      maxHeight: '70%',
      backgroundColor: colors.surface,
      borderRadius: radius.card,
      padding: spacing.lg,
    },
    header: {
      flexDirection: 'row',
      alignItems: 'center',
      justifyContent: 'space-between',
      marginBottom: spacing.sm,
    },
    title: {
      ...typography.h2,
      color: colors.black,
    },
    markAll: {
      color: colors.blue,
      fontSize: 12,
      fontWeight: '600',
    },
    pushBox: {
      backgroundColor: colors.blueSoft,
      borderRadius: radius.card,
      padding: spacing.md,
      marginBottom: spacing.sm,
      gap: spacing.xs,
    },
    pushText: {
      fontSize: 13,
      lineHeight: 18,
      color: colors.black,
    },
    pushAction: {
      color: colors.blue,
      fontSize: 13,
      fontWeight: '700',
    },
    pushResult: {
      fontSize: 12,
      color: colors.textMuted,
    },
    list: {
      marginTop: spacing.xs,
    },
    empty: {
      textAlign: 'center',
      color: colors.textMuted,
      paddingVertical: spacing.lg,
    },
    item: {
      flexDirection: 'row',
      alignItems: 'center',
      gap: spacing.sm,
      paddingVertical: spacing.sm,
      borderTopWidth: 1,
      borderTopColor: colors.line,
    },
    itemPressed: {
      opacity: 0.6,
    },
    unreadDot: {
      width: 8,
      height: 8,
      borderRadius: 4,
      backgroundColor: colors.danger,
      marginTop: 6,
      alignSelf: 'flex-start',
    },
    dotSpacer: {
      width: 8,
    },
    itemBody: {
      flex: 1,
    },
    message: {
      fontSize: 14,
      color: colors.black,
      lineHeight: 20,
    },
    date: {
      fontSize: 11,
      color: colors.textMuted,
      marginTop: 2,
    },
  }));
  const { notifications, unreadCount, markAllRead, markRead, pushState, enablePush, sendTestPush } =
    useNotifications();
  const [pushBusy, setPushBusy] = useState(false);
  const [pushResult, setPushResult] = useState<string | null>(null);

  async function handleEnable() {
    setPushBusy(true);
    setPushResult(null);
    try {
      await enablePush();
    } finally {
      setPushBusy(false);
    }
  }

  async function handleTest() {
    setPushBusy(true);
    setPushResult(null);
    try {
      const delivered = await sendTestPush();
      setPushResult(
        delivered > 0
          ? `Enviada para ${delivered} aparelho${delivered > 1 ? 's' : ''}. Deve chegar em instantes.`
          : 'Nenhum aparelho recebeu. Toque em "Ativar notificações" de novo ou reabra o app.',
      );
    } catch {
      setPushResult('Não foi possível enviar o teste agora.');
    } finally {
      setPushBusy(false);
    }
  }

  function handlePress(item: NotificationItem) {
    onClose();
    markRead(item.id);
    const route = NOTIFICATION_ROUTES[item.type];
    if (route) {
      router.push(route as never);
    }
  }

  return (
    <Modal visible={visible} transparent animationType="fade" onRequestClose={onClose}>
      <Pressable style={styles.backdrop} onPress={onClose}>
        <Pressable style={styles.panel} onPress={() => {}}>
          <View style={styles.header}>
            <Text style={styles.title}>Notificações</Text>
            {unreadCount > 0 ? (
              <Pressable onPress={markAllRead}>
                <Text style={styles.markAll}>Marcar tudo como lido</Text>
              </Pressable>
            ) : null}
          </View>

          {pushState === 'prompt' ? (
            <View style={styles.pushBox}>
              <Text style={styles.pushText}>
                Receba avisos de agendamento neste aparelho, mesmo com o app fechado.
              </Text>
              <Pressable onPress={handleEnable} disabled={pushBusy}>
                <Text style={styles.pushAction}>{pushBusy ? 'Ativando…' : 'Ativar notificações'}</Text>
              </Pressable>
            </View>
          ) : null}
          {pushState === 'denied' ? (
            <View style={styles.pushBox}>
              <Text style={styles.pushText}>
                As notificações estão bloqueadas neste navegador. Libere nas configurações do site
                (cadeado ao lado do endereço) e recarregue a página.
              </Text>
            </View>
          ) : null}
          {pushState === 'needs-install' ? (
            <View style={styles.pushBox}>
              <Text style={styles.pushText}>
                No iPhone, as notificações só funcionam com o app na Tela de Início: toque em
                Compartilhar → Adicionar à Tela de Início e abra o Seu Barbeiro por lá.
              </Text>
            </View>
          ) : null}
          {pushState === 'insecure' ? (
            <View style={styles.pushBox}>
              <Text style={styles.pushText}>
                Notificações no celular exigem conexão segura (https). Acesse pelo endereço oficial
                do app.
              </Text>
            </View>
          ) : null}
          {pushState === 'unsupported' ? (
            <View style={styles.pushBox}>
              <Text style={styles.pushText}>
                Este navegador não suporta notificações push. Os avisos continuam aparecendo aqui
                dentro.
              </Text>
            </View>
          ) : null}
          {pushState === 'ready' ? (
            <View style={styles.pushBox}>
              <Pressable onPress={handleTest} disabled={pushBusy}>
                <Text style={styles.pushAction}>
                  {pushBusy ? 'Enviando…' : 'Notificações ativas · enviar teste'}
                </Text>
              </Pressable>
              {pushResult ? <Text style={styles.pushResult}>{pushResult}</Text> : null}
            </View>
          ) : null}

          <FlatList
            data={notifications}
            keyExtractor={(item) => String(item.id)}
            style={styles.list}
            ListEmptyComponent={<Text style={styles.empty}>Nenhuma notificação ainda.</Text>}
            renderItem={({ item }) => (
              <Pressable
                style={({ pressed }) => [styles.item, pressed && styles.itemPressed]}
                onPress={() => handlePress(item)}
              >
                {!item.read ? <View style={styles.unreadDot} /> : <View style={styles.dotSpacer} />}
                <View style={styles.itemBody}>
                  <Text style={styles.message}>{item.message}</Text>
                  <Text style={styles.date}>{formatDateTime(item.createdAt)}</Text>
                </View>
                {NOTIFICATION_ROUTES[item.type] ? (
                  <Ionicons name="chevron-forward" size={16} color={colors.textFaint} />
                ) : null}
              </Pressable>
            )}
          />
        </Pressable>
      </Pressable>
    </Modal>
  );
}
