import { Ionicons } from '@expo/vector-icons';
import { Redirect, Tabs } from 'expo-router';
import { StyleSheet, View } from 'react-native';

import { useAuth } from '../../context/AuthContext';
import { useNotifications } from '../../context/NotificationContext';
import { colors } from '../../theme/colors';
import { fonts } from '../../theme/typography';

export default function AppLayout() {
  const { session, isLoading } = useAuth();
  const { unreadCount } = useNotifications();

  if (isLoading) {
    return null;
  }

  if (!session) {
    return <Redirect href="/(auth)/login" />;
  }

  return (
    <Tabs
      screenOptions={{
        headerShown: false,
        tabBarActiveTintColor: colors.black,
        tabBarInactiveTintColor: colors.textFaint,
        tabBarLabelStyle: { fontFamily: fonts.headingMedium, fontSize: 11 },
        tabBarStyle: { borderTopColor: colors.pillBorder },
      }}
    >
      <Tabs.Screen
        name="home"
        options={{
          title: session.role === 'CLIENT' ? 'Barbearias' : 'Início',
          tabBarIcon: ({ color, size }) => <Ionicons name="home" color={color} size={size} />,
        }}
      />
      <Tabs.Screen
        name="appointments"
        options={{
          title: 'Agendamentos',
          tabBarIcon: ({ color, size }) => (
            <View>
              <Ionicons name="calendar" color={color} size={size} />
              {unreadCount > 0 ? <View style={styles.tabDot} /> : null}
            </View>
          ),
        }}
      />
      <Tabs.Screen
        name="profile"
        options={{
          title: 'Perfil',
          tabBarIcon: ({ color, size }) => <Ionicons name="person" color={color} size={size} />,
        }}
      />
      <Tabs.Screen name="shop/[id]" options={{ href: null }} />
      <Tabs.Screen name="book/[shopId]" options={{ href: null }} />
      <Tabs.Screen name="appointment/[id]/pix" options={{ href: null }} />
      <Tabs.Screen name="availability" options={{ href: null }} />
      <Tabs.Screen name="blocked-clients" options={{ href: null }} />
      <Tabs.Screen name="services" options={{ href: null }} />
      <Tabs.Screen name="products" options={{ href: null }} />
      <Tabs.Screen name="subscription" options={{ href: null }} />
    </Tabs>
  );
}

const styles = StyleSheet.create({
  tabDot: {
    position: 'absolute',
    top: -2,
    right: -6,
    width: 8,
    height: 8,
    borderRadius: 4,
    backgroundColor: colors.danger,
  },
});
