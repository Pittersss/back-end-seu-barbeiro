import { Ionicons } from '@expo/vector-icons';
import { router } from 'expo-router';
import { FlatList, Modal, Pressable, StyleSheet, Text, View } from 'react-native';

import { useNotifications } from '../context/NotificationContext';
import { formatDateTime } from '../lib/format';
import type { NotificationItem, NotificationType } from '../lib/types';
import { colors } from '../theme/colors';
import { radius, spacing } from '../theme/spacing';
import { typography } from '../theme/typography';

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
  const { notifications, unreadCount, markAllRead, markRead } = useNotifications();

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

const styles = StyleSheet.create({
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
});
