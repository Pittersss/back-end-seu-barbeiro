import { Image, StyleSheet } from 'react-native';

import { useTheme } from '../theme/ThemeContext';

// Both crests are transparent PNGs (~1.198:1). Despite the names, `_dark` is the black-ink
// crest meant for the light theme and `_light` is the white-ink crest for the dark theme.
const LOGO_ASPECT_RATIO = 1373 / 1146;
const logoForLightTheme = require('../assets/your_barber_logo_dark.png');
const logoForDarkTheme = require('../assets/your_barber_logo_light.png');

export function Logo({ size = 120 }: { size?: number }) {
  const { isDark } = useTheme();
  return (
    <Image
      source={isDark ? logoForDarkTheme : logoForLightTheme}
      resizeMode="contain"
      style={[styles.image, { width: size, height: size / LOGO_ASPECT_RATIO }]}
    />
  );
}

const styles = StyleSheet.create({
  image: {
    alignSelf: 'center',
  },
});
