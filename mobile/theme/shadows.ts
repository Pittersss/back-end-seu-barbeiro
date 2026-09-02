import { Platform, type ViewStyle } from 'react-native';

// Soft, neutral elevations. On web, react-native-web (0.21) understands the
// `boxShadow` string; on native we use the classic shadow* props plus Android
// `elevation`.
export const shadows: { sm: ViewStyle; card: ViewStyle } = {
  sm: Platform.select({
    web: { boxShadow: '0 1px 2px rgba(10,10,10,0.05), 0 2px 8px rgba(10,10,10,0.04)' },
    default: {
      shadowColor: '#0A0A0A',
      shadowOffset: { width: 0, height: 2 },
      shadowOpacity: 0.05,
      shadowRadius: 6,
      elevation: 1,
    },
  }) as ViewStyle,
  card: Platform.select({
    web: { boxShadow: '0 1px 3px rgba(10,10,10,0.06), 0 10px 24px rgba(10,10,10,0.06)' },
    default: {
      shadowColor: '#0A0A0A',
      shadowOffset: { width: 0, height: 6 },
      shadowOpacity: 0.07,
      shadowRadius: 16,
      elevation: 3,
    },
  }) as ViewStyle,
};
