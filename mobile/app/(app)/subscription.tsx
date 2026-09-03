import { Ionicons } from '@expo/vector-icons';
import * as Clipboard from 'expo-clipboard';
import { router, useFocusEffect } from 'expo-router';
import { useCallback, useState } from 'react';
import { ActivityIndicator, Image, Pressable, ScrollView, StyleSheet, Text, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';

import { Button } from '../../components/Button';
import { Card } from '../../components/Card';
import { ApiError } from '../../lib/api';
import { getSubscriptionStatus, requestSubscriptionPix } from '../../lib/api/subscriptions';
import { formatCurrency } from '../../lib/format';
import type { PixQrCodeResponse, SubscriptionStatus } from '../../lib/types';
import { colors } from '../../theme/colors';
import { centeredPage } from '../../theme/layout';
import { radius, spacing } from '../../theme/spacing';
import { typography } from '../../theme/typography';

/** `periodEnd` is a date-only string (`YYYY-MM-DD`); reformat it as text, no `Date` parsing
 * (parsing a date-only ISO string as UTC and rendering it in a negative-offset timezone like
 * Brazil's shifts it back a day). */
function formatDateOnly(dateOnly: string): string {
  const [year, month, day] = dateOnly.split('-');
  return `${day}/${month}/${year}`;
}

export default function SubscriptionScreen() {
  const [status, setStatus] = useState<SubscriptionStatus | null>(null);
  const [periodEnd, setPeriodEnd] = useState<string | null>(null);
  const [pix, setPix] = useState<PixQrCodeResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [generating, setGenerating] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [copied, setCopied] = useState(false);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await getSubscriptionStatus();
      setStatus(data.status);
      setPeriodEnd(data.periodEnd ?? null);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Não foi possível carregar a assinatura.');
    } finally {
      setLoading(false);
    }
  }, []);

  useFocusEffect(
    useCallback(() => {
      load();
    }, [load]),
  );

  async function handleGeneratePix() {
    setError(null);
    setGenerating(true);
    try {
      const data = await requestSubscriptionPix();
      setPix(data);
      setStatus('PENDING_CONFIRMATION');
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Não foi possível gerar o código Pix.');
    } finally {
      setGenerating(false);
    }
  }

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

  return (
    <SafeAreaView style={styles.safe} edges={['top']}>
      <View style={styles.header}>
        <Pressable onPress={() => router.back()} hitSlop={12}>
          <Ionicons name="chevron-back" size={24} color={colors.black} />
        </Pressable>
        <Text style={styles.headerTitle}>Assinatura</Text>
        <View style={{ width: 24 }} />
      </View>

      <ScrollView contentContainerStyle={styles.content}>
        {error ? <Text style={styles.error}>{error}</Text> : null}

        {status === 'ACTIVE' ? (
          <Card style={styles.statusCard}>
            <Ionicons name="checkmark-circle" size={32} color={colors.success} />
            <Text style={styles.statusTitle}>Assinatura ativa</Text>
            {periodEnd ? (
              <Text style={styles.statusText}>Válida até {formatDateOnly(periodEnd)}</Text>
            ) : null}
            <Button
              title="Renovar antecipadamente"
              variant="outline"
              onPress={handleGeneratePix}
              loading={generating}
              style={styles.actionButton}
            />
          </Card>
        ) : null}

        {status === 'PENDING_CONFIRMATION' && !pix ? (
          <Card style={styles.statusCard}>
            <Ionicons name="time-outline" size={32} color={colors.textMuted} />
            <Text style={styles.statusTitle}>Pagamento em análise</Text>
            <Text style={styles.statusText}>
              Assim que o pagamento for confirmado, sua assinatura será ativada.
            </Text>
          </Card>
        ) : null}

        {status === 'INACTIVE' && !pix ? (
          <Card style={styles.statusCard}>
            <Ionicons name="lock-closed-outline" size={32} color={colors.textMuted} />
            <Text style={styles.statusTitle}>Assinatura inativa</Text>
            <Text style={styles.statusText}>
              Assine por {formatCurrency(30)} por mês para gerenciar serviços, produtos,
              disponibilidade e agendamentos.
            </Text>
            <Button
              title="Gerar Pix"
              onPress={handleGeneratePix}
              loading={generating}
              style={styles.actionButton}
            />
          </Card>
        ) : null}

        {pix ? (
          <>
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
              <Ionicons name={copied ? 'checkmark' : 'copy-outline'} size={18} color={colors.black} />
            </Pressable>
            {copied ? <Text style={styles.copiedHint}>Copiado!</Text> : null}
          </>
        ) : null}
      </ScrollView>
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
  header: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingHorizontal: spacing.lg,
    paddingTop: spacing.sm,
    ...centeredPage,
  },
  headerTitle: {
    ...typography.h2,
    color: colors.black,
  },
  content: {
    padding: spacing.lg,
    paddingBottom: spacing.xxl,
    alignItems: 'center',
    ...centeredPage,
  },
  error: {
    color: colors.red,
    textAlign: 'center',
    marginBottom: spacing.md,
  },
  statusCard: {
    alignItems: 'center',
    gap: spacing.sm,
    padding: spacing.lg,
    width: '100%',
    marginBottom: spacing.lg,
  },
  statusTitle: {
    ...typography.h2,
    color: colors.black,
  },
  statusText: {
    fontSize: 14,
    color: colors.textMuted,
    textAlign: 'center',
  },
  actionButton: {
    marginTop: spacing.sm,
    alignSelf: 'stretch',
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
  },
  qr: {
    width: 220,
    height: 220,
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
});
