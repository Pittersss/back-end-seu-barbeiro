import { Ionicons } from '@expo/vector-icons';
import * as Clipboard from 'expo-clipboard';
import { router, useFocusEffect, useLocalSearchParams } from 'expo-router';
import { useCallback, useState } from 'react';
import { ActivityIndicator, Image, Pressable, ScrollView, StyleSheet, Text, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';

import { Button } from '../../../../components/Button';
import { getPixQrCode } from '../../../../lib/api/pix';
import { formatCurrency } from '../../../../lib/format';
import type { PixQrCodeResponse } from '../../../../lib/types';
import { colors } from '../../../../theme/colors';
import { centeredPage } from '../../../../theme/layout';
import { radius, spacing } from '../../../../theme/spacing';
import { typography } from '../../../../theme/typography';

export default function PixScreen() {
  const { id } = useLocalSearchParams<{ id: string }>();
  const appointmentId = Number(id);

  const [pix, setPix] = useState<PixQrCodeResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [copied, setCopied] = useState(false);

  useFocusEffect(
    useCallback(() => {
      (async () => {
        setLoading(true);
        setPix(await getPixQrCode(appointmentId));
        setLoading(false);
      })();
    }, [appointmentId]),
  );

  async function handleCopy() {
    if (!pix) return;
    await Clipboard.setStringAsync(pix.pixCopyPaste);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  }

  if (loading || !pix) {
    return (
      <SafeAreaView style={styles.safe}>
        <ActivityIndicator style={styles.loading} color={colors.black} />
      </SafeAreaView>
    );
  }

  return (
    <SafeAreaView style={styles.safe} edges={['top']}>
      <ScrollView contentContainerStyle={styles.content}>
        <Text style={styles.title}>Pague com Pix</Text>
        <Text style={styles.amount}>{formatCurrency(pix.amount)}</Text>

        <View style={styles.qrWrap}>
          <Image
            source={{ uri: `data:image/png;base64,${pix.qrCodeBase64}` }}
            style={styles.qr}
            resizeMode="contain"
          />
        </View>

        <Text style={styles.label}>Pix copia e cola</Text>
        <Pressable style={styles.copyBox} onPress={handleCopy}>
          <Text style={styles.copyText} numberOfLines={2}>
            {pix.pixCopyPaste}
          </Text>
          <Ionicons name={copied ? 'checkmark' : 'copy-outline'} size={18} color={colors.black} />
        </Pressable>
        {copied ? <Text style={styles.copiedHint}>Copiado!</Text> : null}

        <View style={styles.merchantCard}>
          <Text style={styles.merchantRow}>Recebedor: {pix.merchantName}</Text>
          <Text style={styles.merchantRow}>Cidade: {pix.merchantCity}</Text>
          <Text style={styles.merchantRow}>Chave Pix: {pix.pixKey}</Text>
        </View>
      </ScrollView>

      <View style={styles.footer}>
        <Button title="Ver meus agendamentos" onPress={() => router.replace('/(app)/appointments')} />
      </View>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  safe: {
    flex: 1,
    backgroundColor: colors.white,
  },
  loading: {
    flex: 1,
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
  qrWrap: {
    width: 220,
    height: 220,
    borderRadius: radius.card,
    borderWidth: 1,
    borderColor: colors.pillBorder,
    alignItems: 'center',
    justifyContent: 'center',
    marginBottom: spacing.lg,
  },
  qr: {
    width: 200,
    height: 200,
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
    gap: spacing.sm,
  },
  copyText: {
    flex: 1,
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
    borderTopColor: colors.pillBorder,
    ...centeredPage,
  },
});
