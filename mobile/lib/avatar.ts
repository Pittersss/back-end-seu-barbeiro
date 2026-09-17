import * as ImageManipulator from 'expo-image-manipulator';
import * as ImagePicker from 'expo-image-picker';

/**
 * Opens the photo library, lets the user crop a square, then downscales to
 * 192px JPEG and returns raw base64 (no data-URI prefix) ready for
 * `PUT /api/users/me`. Returns null if the user cancels or denies access.
 *
 * 192px at quality 0.5 is still >2x oversampled for the largest avatar this
 * app renders (92px, in profile.tsx) — kept small because avatars are stored
 * as base64 TEXT directly in Postgres (see AGENTS.md), and the production DB
 * (Neon free tier) caps out at 0.5GB.
 */
export async function pickAvatarBase64(): Promise<string | null> {
  const permission = await ImagePicker.requestMediaLibraryPermissionsAsync();
  if (!permission.granted) return null;

  const result = await ImagePicker.launchImageLibraryAsync({
    mediaTypes: ['images'],
    allowsEditing: true,
    aspect: [1, 1],
    quality: 1,
  });
  if (result.canceled || !result.assets?.length) return null;

  const manipulated = await ImageManipulator.manipulateAsync(
    result.assets[0].uri,
    [{ resize: { width: 192, height: 192 } }],
    { compress: 0.5, format: ImageManipulator.SaveFormat.JPEG, base64: true },
  );
  return manipulated.base64 ?? null;
}
