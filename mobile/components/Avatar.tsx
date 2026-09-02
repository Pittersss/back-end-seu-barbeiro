import {
  Image,
  StyleSheet,
  Text,
  View,
  type ImageStyle,
  type StyleProp,
  type ViewStyle,
} from 'react-native';

import { initials } from '../lib/format';
import { colors } from '../theme/colors';
import { fonts } from '../theme/typography';

interface AvatarProps {
  name?: string;
  /** Raw base64 (no data-URI prefix) or a full uri/data-URI string. */
  avatarBase64?: string | null;
  size?: number;
  tone?: 'blue' | 'black';
  style?: StyleProp<ViewStyle>;
}

function toUri(value: string): string {
  if (value.startsWith('data:') || value.startsWith('http') || value.startsWith('file:')) {
    return value;
  }
  return `data:image/jpeg;base64,${value}`;
}

export function Avatar({ name, avatarBase64, size = 44, tone = 'blue', style }: AvatarProps) {
  const dimension = { width: size, height: size, borderRadius: size / 2 };

  if (avatarBase64) {
    return (
      <Image
        source={{ uri: toUri(avatarBase64) }}
        style={[styles.image, dimension, style] as StyleProp<ImageStyle>}
      />
    );
  }

  return (
    <View
      style={[
        styles.fallback,
        dimension,
        { backgroundColor: tone === 'black' ? colors.black : colors.blue },
        style,
      ]}
    >
      <Text style={[styles.text, { fontSize: size * 0.38 }]}>
        {name ? initials(name) : '?'}
      </Text>
    </View>
  );
}

const styles = StyleSheet.create({
  image: {
    backgroundColor: colors.pill,
  },
  fallback: {
    alignItems: 'center',
    justifyContent: 'center',
  },
  text: {
    color: colors.white,
    fontFamily: fonts.heading,
    letterSpacing: 0.5,
  },
});
