import { Ionicons } from '@expo/vector-icons';
import * as Clipboard from 'expo-clipboard';
import { router, useFocusEffect, useLocalSearchParams } from 'expo-router';
import { useCallback, useState } from 'react';
import { ActivityIndicator, Image, Pressable, ScrollView, Text, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';

import { Button } from '../../../../components/Button';
import { Card } from '../../../../components/Card';
import { ApiError } from '../../../../lib/api';
import { getPixQrCode } from '../../../../lib/api/pix';
import { formatCurrency } from '../../../../lib/format';
import type { PixQrCodeResponse } from '../../../../lib/types';
import { centeredPage } from '../../../../theme/layout';
import { radius, spacing } from '../../../../theme/spacing';
import { useThemeColors } from '../../../../theme/ThemeContext';
import { typography } from '../../../../theme/typography';
import { useThemedStyles } from '../../../../theme/useThemedStyles';

export default function PixScreen() {
  const colors = useThemeColors();
  const styles = useThemedStyles((colors) => ({
    safe: {
      flex: 1,
      backgroundColor: colors.bg,
    },
    loading: {
      flex: 1,
    },
    errorWrap: {
      flex: 1,
      alignItems: 'center',
      justifyContent: 'center',
      gap: spacing.md,
      padding: spacing.lg,
      ...centeredPage,
    },
    errorText: {
      color: colors.textMuted,
      textAlign: 'center',
      fontSize: 14,
    },
    content: {
      padding: spacing.lg,
      alignItems: 'center',
      ...centeredPage,
    },
    title: {
      ...typography.h1,
      color: colors.black,
    },
    amount: {
      fontFamily: typography.h1.fontFamily,
      fontSize: 32,
      color: colors.blue,
      marginTop: spacing.xs,
      marginBottom: spacing.lg,
    },
    qrCard: {
      alignItems: 'center',
      justifyContent: 'center',
      padding: spacing.md,
      marginBottom: spacing.lg,
      // Scanners need dark-on-light with a quiet zone, whatever the app theme is.
      backgroundColor: '#FFFFFF',
    },
    qr: {
      width: 240,
      height: 240,
      backgroundColor: '#FFFFFF',
    },
    label: {
      ...typography.label,
      color: colors.textMuted,
      alignSelf: 'flex-start',
      marginBottom: spacing.xs,
    },
    copyBox: {
      flexDirection: 'row',
      alignItems: 'center',
      justifyContent: 'space-between',
      backgroundColor: colors.pill,
      borderRadius: radius.card,
      padding: spacing.md,
      width: '100%',
      // A few px wider than the column so the copy icon has room.
      marginHorizontal: -spacing.xs,
      gap: spacing.sm,
    },
    copyText: {
      flex: 1,
      // Without minWidth 0 the long unbroken payload refuses to shrink and pushes the icon out.
      minWidth: 0,
      fontSize: 12,
      color: colors.black,
    },
    copiedHint: {
      color: colors.success,
      fontSize: 12,
      marginTop: 4,
      alignSelf: 'flex-start',
    },
    merchantCard: {
      width: '100%',
      marginTop: spacing.lg,
      gap: 4,
    },
    merchantRow: {
      fontSize: 13,
      color: colors.textMuted,
    },
    footer: {
      padding: spacing.lg,
      borderTopWidth: 1,
      borderTopColor: colors.line,
      backgroundColor: colors.surface,
      ...centeredPage,
    },
  }));
  const { id } = useLocalSearchParams<{ id: string }>();
  const appointmentId = Number(id);

  const [pix, setPix] = useState<PixQrCodeResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [copied, setCopied] = useState(false);

  useFocusEffect(
    useCallback(() => {
      let cancelled = false;
      (async () => {
        setLoading(true);
        setError(null);
        try {
          const data = await getPixQrCode(appointmentId);
          if (!cancelled) setPix(data);
        } catch (err) {
          if (!cancelled) {
            setError(
              err instanceof ApiError
                ? err.message
                : 'Não foi possível gerar o código Pix.',
            );
          }
        } finally {
          if (!cancelled) setLoading(false);
        }
      })();
      return () => {
        cancelled = true;
      };
    }, [appointmentId]),
  );

  async function handleCopy() {
    if (!pix) return;
    await Clipboard.setStringAsync(pix.pixCopyPaste);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  }

  if (loading) {
    return (
      <SafeAreaView style={styles.safe}>
        <ActivityIndicator style={styles.loading} color={colors.black} />
      </SafeAreaView>
    );
  }

  if (error || !pix) {
    return (
      <SafeAreaView style={styles.safe} edges={['top']}>
        <View style={styles.errorWrap}>
          <Ionicons name="alert-circle-outline" size={40} color={colors.textMuted} />
          <Text style={styles.errorText}>{error ?? 'Código Pix indisponível.'}</Text>
          <Button
            title="Ver meus agendamentos"
            variant="outline"
            onPress={() => router.replace('/(app)/appointments')}
          />
        </View>
      </SafeAreaView>
    );
  }

  return (
    <SafeAreaView style={styles.safe} edges={['top']}>
      <ScrollView contentContainerStyle={styles.content}>
        <Text style={styles.title}>Pague com Pix</Text>
        <Text style={styles.amount}>{formatCurrency(pix.amount)}</Text>

        <Card style={styles.qrCard}>
          <Image
            source={{ uri: `data:image/png;base64,${pix.qrCodeBase64}` }}
            style={styles.qr}
            resizeMode="contain"
          />
        </Card>

        <Text style={styles.label}>Pix copia e cola</Text>
        <Pressable style={styles.copyBox} onPress={handleCopy}>
          <Text style={styles.copyText} numberOfLines={2}>
            {pix.pixCopyPaste}
          </Text>
          <Ionicons
            name={copied ? 'checkmark' : 'copy-outline'}
            size={18}
            color={colors.black}
            style={{ flexShrink: 0 }}
          />
        </Pressable>
        {copied ? <Text style={styles.copiedHint}>Copiado!</Text> : null}

        <Card style={styles.merchantCard} padded>
          <Text style={styles.merchantRow}>Recebedor: {pix.merchantName}</Text>
          <Text style={styles.merchantRow}>Cidade: {pix.merchantCity}</Text>
          <Text style={styles.merchantRow}>Chave Pix: {pix.pixKey}</Text>
        </Card>
      </ScrollView>

      <View style={styles.footer}>
        <Button title="Ver meus agendamentos" onPress={() => router.replace('/(app)/appointments')} />
      </View>
    </SafeAreaView>
  );
}
