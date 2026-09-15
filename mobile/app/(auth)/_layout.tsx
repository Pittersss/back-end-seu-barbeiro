import { Stack } from 'expo-router';

import { useThemeColors } from '../../theme/ThemeContext';

export default function AuthLayout() {
  const colors = useThemeColors();
  return (
    <Stack
      screenOptions={{
        headerShown: false,
        contentStyle: { backgroundColor: colors.bg },
      }}
    />
  );
}
