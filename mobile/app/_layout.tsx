import { Oswald_500Medium, Oswald_700Bold, useFonts } from '@expo-google-fonts/oswald';
import { Stack } from 'expo-router';
import { StatusBar } from 'expo-status-bar';
import * as SplashScreen from 'expo-splash-screen';
import { useEffect } from 'react';
import { SafeAreaProvider } from 'react-native-safe-area-context';

import { AuthProvider } from '../context/AuthContext';
import { NotificationProvider } from '../context/NotificationContext';
import { ThemeProvider, useTheme } from '../theme/ThemeContext';

SplashScreen.preventAutoHideAsync().catch(() => {});

export default function RootLayout() {
  const [fontsLoaded] = useFonts({
    Oswald_700Bold,
    Oswald_500Medium,
  });

  useEffect(() => {
    if (fontsLoaded) {
      SplashScreen.hideAsync().catch(() => {});
    }
  }, [fontsLoaded]);

  if (!fontsLoaded) {
    return null;
  }

  return (
    <SafeAreaProvider>
      <ThemeProvider>
        <AuthProvider>
          <NotificationProvider>
            <RootNavigator />
          </NotificationProvider>
        </AuthProvider>
      </ThemeProvider>
    </SafeAreaProvider>
  );
}

function RootNavigator() {
  const { colors, isDark } = useTheme();
  return (
    <>
      <StatusBar style={isDark ? 'light' : 'dark'} />
      <Stack
        screenOptions={{
          headerShown: false,
          contentStyle: { backgroundColor: colors.bg },
        }}
      />
    </>
  );
}
