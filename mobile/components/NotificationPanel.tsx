import { FlatList, Modal, Pressable, StyleSheet, Text, View } from 'react-native';

import { useNotifications } from '../context/NotificationContext';
import { formatDateTime } from '../lib/format';
import { colors } from '../theme/colors';
import { radius, spacing } from '../theme/spacing';
import { typography } from '../theme/typography';

interface NotificationPanelProps {
  visible: boolean;
  onClose: () => void;
}

export function NotificationPanel({ visible, onClose }: NotificationPanelProps) {
  const { notifications, unreadCount, markAllRead } = useNotifications();

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
              <View style={styles.item}>
                {!item.read ? <View style={styles.unreadDot} /> : <View style={styles.dotSpacer} />}
                <View style={styles.itemBody}>
                  <Text style={styles.message}>{item.message}</Text>
                  <Text style={styles.date}>{formatDateTime(item.createdAt)}</Text>
                </View>
              </View>
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
    gap: spacing.sm,
    paddingVertical: spacing.sm,
    borderTopWidth: 1,
    borderTopColor: colors.line,
  },
  unreadDot: {
    width: 8,
    height: 8,
    borderRadius: 4,
    backgroundColor: colors.danger,
    marginTop: 6,
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
