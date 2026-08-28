import { Image, StyleSheet } from 'react-native';

// Real exported crest — 1373x1146 (~1.198:1 aspect ratio).
const LOGO_ASPECT_RATIO = 1373 / 1146;
const logoSource = require('../assets/your_barber_logo.png');

export function Logo({ size = 120 }: { size?: number }) {
  return (
    <Image
      source={logoSource}
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
