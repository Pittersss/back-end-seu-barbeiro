import { Platform, type ViewStyle } from 'react-native';

// Barely-there elevations. On a tinted canvas with hairline borders, a card
// only needs the faintest lift — heavy shadows are the opposite of the look
// we're going for. On web, react-native-web (0.21) understands the `boxShadow`
// string; on native we use the classic shadow* props plus Android `elevation`.
export const shadows: { sm: ViewStyle; card: ViewStyle } = {
  sm: Platform.select({
    web: { boxShadow: '0 1px 2px rgba(10,10,10,0.04)' },
    default: {
      shadowColor: '#0A0A0A',
      shadowOffset: { width: 0, height: 1 },
      shadowOpacity: 0.04,
      shadowRadius: 4,
      elevation: 1,
    },
  }) as ViewStyle,
  card: Platform.select({
    web: { boxShadow: '0 1px 2px rgba(10,10,10,0.04), 0 6px 20px rgba(10,10,10,0.05)' },
    default: {
      shadowColor: '#0A0A0A',
      shadowOffset: { width: 0, height: 4 },
      shadowOpacity: 0.05,
      shadowRadius: 12,
      elevation: 2,
    },
  }) as ViewStyle,
};
