import {
  Image,
  Text,
  View,
  type ImageStyle,
  type StyleProp,
  type ViewStyle,
} from 'react-native';

import { initials } from '../lib/format';
import { useThemeColors } from '../theme/ThemeContext';
import { fonts } from '../theme/typography';
import { useThemedStyles } from '../theme/useThemedStyles';

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
  const colors = useThemeColors();
  const styles = useThemedStyles((colors) => ({
    image: {
      backgroundColor: colors.pill,
    },
    fallback: {
      alignItems: 'center',
      justifyContent: 'center',
    },
    text: {
      fontFamily: fonts.heading,
      letterSpacing: 0.5,
    },
  }));
  const dimension = { width: size, height: size, borderRadius: size / 2 };

  if (avatarBase64) {
    return (
      <Image
        source={{ uri: toUri(avatarBase64) }}
        style={[styles.image, dimension, style] as StyleProp<ImageStyle>}
      />
    );
  }

  // "black" tone fills with the flipping ink color, so its label must flip to the
  // paired paper color to stay legible; "blue" tone is a fixed accent fill, so its
  // label stays fixed white regardless of scheme.
  const fillColor = tone === 'black' ? colors.black : colors.blue;
  const labelColor = tone === 'black' ? colors.white : colors.onAccent;

  return (
    <View style={[styles.fallback, dimension, { backgroundColor: fillColor }, style]}>
      <Text style={[styles.text, { fontSize: size * 0.38, color: labelColor }]}>
        {name ? initials(name) : '?'}
      </Text>
    </View>
  );
}
