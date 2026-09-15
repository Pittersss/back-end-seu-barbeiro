import { Ionicons } from '@expo/vector-icons';
import { useState } from 'react';
import { Pressable, Text, View } from 'react-native';

import { useNotifications } from '../context/NotificationContext';
import { useThemeColors } from '../theme/ThemeContext';
import { useThemedStyles } from '../theme/useThemedStyles';
import { NotificationPanel } from './NotificationPanel';

export function NotificationBell() {
  const colors = useThemeColors();
  const styles = useThemedStyles((colors) => ({
    button: {
      position: 'relative',
    },
    badge: {
      position: 'absolute',
      top: -4,
      right: -6,
      minWidth: 16,
      height: 16,
      borderRadius: 8,
      paddingHorizontal: 3,
      backgroundColor: colors.danger,
      alignItems: 'center',
      justifyContent: 'center',
    },
    badgeText: {
      color: colors.onAccent,
      fontSize: 10,
      fontWeight: '700',
    },
  }));
  const { unreadCount } = useNotifications();
  const [open, setOpen] = useState(false);

  return (
    <>
      <Pressable onPress={() => setOpen(true)} style={styles.button} hitSlop={8}>
        <Ionicons name="notifications-outline" size={22} color={colors.black} />
        {unreadCount > 0 ? (
          <View style={styles.badge}>
            <Text style={styles.badgeText}>{unreadCount > 9 ? '9+' : unreadCount}</Text>
          </View>
        ) : null}
      </Pressable>
      <NotificationPanel visible={open} onClose={() => setOpen(false)} />
    </>
  );
}
