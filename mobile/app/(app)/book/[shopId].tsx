import { Ionicons } from '@expo/vector-icons';
import DateTimePicker from '@react-native-community/datetimepicker';
import { router, useFocusEffect, useLocalSearchParams } from 'expo-router';
import { useCallback, useState } from 'react';
import { ActivityIndicator, Platform, Pressable, ScrollView, StyleSheet, Text, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';

import { Button } from '../../../components/Button';
import { createAppointment } from '../../../lib/api/appointments';
import { ApiError } from '../../../lib/api';
import { listShopBarbers } from '../../../lib/api/barbershops';
import { listServices } from '../../../lib/api/services';
import { formatCurrency, formatDateTime, formatDuration, initials } from '../../../lib/format';
import type { Barber, PaymentMethod, Service } from '../../../lib/types';
import { colors } from '../../../theme/colors';
import { centeredPage } from '../../../theme/layout';
import { radius, spacing } from '../../../theme/spacing';
import { typography } from '../../../theme/typography';

const PAYMENT_OPTIONS: { value: PaymentMethod; label: string }[] = [
  { value: 'PIX', label: 'Pix' },
  { value: 'CARD', label: 'Cartão' },
  { value: 'CASH', label: 'Dinheiro' },
];

export default function BookingScreen() {
  const { shopId } = useLocalSearchParams<{ shopId: string }>();
  const shopIdNumber = Number(shopId);

  const [services, setServices] = useState<Service[]>([]);
  const [barbers, setBarbers] = useState<Barber[]>([]);
  const [loading, setLoading] = useState(true);

  const [service, setService] = useState<Service | null>(null);
  const [barber, setBarber] = useState<Barber | null>(null);
  const [scheduledAt, setScheduledAt] = useState<Date>(() => {
    const date = new Date();
    date.setDate(date.getDate() + 1);
    date.setHours(10, 0, 0, 0);
    return date;
  });
  const [showDatePicker, setShowDatePicker] = useState(false);
  const [showTimePicker, setShowTimePicker] = useState(false);
  const [paymentMethod, setPaymentMethod] = useState<PaymentMethod>('PIX');
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useFocusEffect(
    useCallback(() => {
      (async () => {
        setLoading(true);
        const [servicesData, barbersData] = await Promise.all([
          listServices(shopIdNumber),
          listShopBarbers(shopIdNumber),
        ]);
        setServices(servicesData.filter((s) => s.available));
        setBarbers(barbersData.filter((b) => b.available));
        setLoading(false);
      })();
    }, [shopIdNumber]),
  );

  async function handleSubmit() {
    if (!service || !barber) return;
    setError(null);
    setSubmitting(true);
    try {
      const appointment = await createAppointment({
        barberId: barber.id,
        serviceId: service.id,
        scheduledAt: scheduledAt.toISOString(),
        paymentMethod,
      });
      if (paymentMethod === 'PIX') {
        router.replace({ pathname: '/(app)/appointment/[id]/pix', params: { id: String(appointment.id) } });
      } else {
        router.replace('/(app)/appointments');
      }
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Não foi possível criar o agendamento.');
    } finally {
      setSubmitting(false);
    }
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
        <Text style={styles.headerTitle}>Agendar Horário</Text>
        <View style={{ width: 24 }} />
      </View>

      <ScrollView contentContainerStyle={styles.content}>
        <Text style={styles.sectionTitle}>Serviço</Text>
        {services.map((item) => (
          <Pressable
            key={item.id}
            style={[styles.optionCard, service?.id === item.id && styles.optionCardSelected]}
            onPress={() => setService(item)}
          >
            <View style={styles.serviceInfo}>
              <Text style={styles.optionTitle}>{item.name}</Text>
              <Text style={styles.optionMeta}>{formatDuration(item.durationMinutes)}</Text>
            </View>
            <Text style={styles.optionPrice}>{formatCurrency(item.price)}</Text>
          </Pressable>
        ))}

        <Text style={styles.sectionTitle}>Barbeiro</Text>
        <View style={styles.barbersRow}>
          {barbers.map((item) => (
            <Pressable
              key={item.id}
              style={styles.barberChip}
              onPress={() => setBarber(item)}
            >
              <View
                style={[
                  styles.barberAvatar,
                  { backgroundColor: barber?.id === item.id ? colors.black : colors.blue },
                ]}
              >
                <Text style={styles.barberAvatarText}>{initials(item.name)}</Text>
              </View>
              <Text style={styles.barberName} numberOfLines={1}>
                {item.name}
              </Text>
            </Pressable>
          ))}
        </View>

        <Text style={styles.sectionTitle}>Data e hora</Text>
        <View style={styles.dateRow}>
          <Pressable style={styles.dateButton} onPress={() => setShowDatePicker(true)}>
            <Ionicons name="calendar-outline" size={16} color={colors.black} />
            <Text style={styles.dateButtonText}>{formatDateTime(scheduledAt.toISOString()).split(' ')[0]}</Text>
          </Pressable>
          <Pressable style={styles.dateButton} onPress={() => setShowTimePicker(true)}>
            <Ionicons name="time-outline" size={16} color={colors.black} />
            <Text style={styles.dateButtonText}>
              {scheduledAt.toLocaleTimeString('pt-BR', { hour: '2-digit', minute: '2-digit' })}
            </Text>
          </Pressable>
        </View>
        {showDatePicker ? (
          <DateTimePicker
            value={scheduledAt}
            mode="date"
            minimumDate={new Date()}
            onChange={(_, date) => {
              setShowDatePicker(Platform.OS === 'ios');
              if (date) {
                const next = new Date(scheduledAt);
                next.setFullYear(date.getFullYear(), date.getMonth(), date.getDate());
                setScheduledAt(next);
              }
            }}
          />
        ) : null}
        {showTimePicker ? (
          <DateTimePicker
            value={scheduledAt}
            mode="time"
            onChange={(_, date) => {
              setShowTimePicker(Platform.OS === 'ios');
              if (date) {
                const next = new Date(scheduledAt);
                next.setHours(date.getHours(), date.getMinutes());
                setScheduledAt(next);
              }
            }}
          />
        ) : null}

        <Text style={styles.sectionTitle}>Pagamento</Text>
        <View style={styles.paymentRow}>
          {PAYMENT_OPTIONS.map((option) => (
            <Pressable
              key={option.value}
              style={[styles.paymentChip, paymentMethod === option.value && styles.paymentChipSelected]}
              onPress={() => setPaymentMethod(option.value)}
            >
              <Text
                style={[styles.paymentChipText, paymentMethod === option.value && styles.paymentChipTextSelected]}
              >
                {option.label}
              </Text>
            </Pressable>
          ))}
        </View>

        {error ? <Text style={styles.error}>{error}</Text> : null}
      </ScrollView>

      <View style={styles.footer}>
        <Button
          title="Confirmar Agendamento"
          loading={submitting}
          disabled={!service || !barber}
          onPress={handleSubmit}
        />
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
    ...centeredPage,
  },
  sectionTitle: {
    ...typography.h2,
    fontSize: 15,
    color: colors.black,
    marginTop: spacing.lg,
    marginBottom: spacing.sm,
  },
  optionCard: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    borderWidth: 1.5,
    borderColor: colors.pillBorder,
    borderRadius: radius.card,
    padding: spacing.md,
    marginBottom: spacing.sm,
  },
  optionCardSelected: {
    borderColor: colors.black,
    backgroundColor: colors.pill,
  },
  serviceInfo: {
    flex: 1,
  },
  optionTitle: {
    fontSize: 15,
    color: colors.black,
    fontWeight: '600',
  },
  optionMeta: {
    fontSize: 12,
    color: colors.textMuted,
    marginTop: 2,
  },
  optionPrice: {
    fontFamily: typography.h2.fontFamily,
    fontSize: 15,
    color: colors.black,
  },
  barbersRow: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: spacing.md,
  },
  barberChip: {
    alignItems: 'center',
    width: 72,
  },
  barberAvatar: {
    width: 56,
    height: 56,
    borderRadius: 28,
    alignItems: 'center',
    justifyContent: 'center',
    marginBottom: 4,
  },
  barberAvatarText: {
    color: colors.white,
    fontFamily: typography.h2.fontFamily,
    fontSize: 16,
  },
  barberName: {
    fontSize: 12,
    color: colors.black,
    textAlign: 'center',
  },
  dateRow: {
    flexDirection: 'row',
    gap: spacing.md,
  },
  dateButton: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 6,
    backgroundColor: colors.pill,
    borderRadius: radius.pill,
    paddingHorizontal: spacing.md,
    paddingVertical: spacing.sm,
  },
  dateButtonText: {
    fontSize: 14,
    color: colors.black,
  },
  paymentRow: {
    flexDirection: 'row',
    gap: spacing.sm,
  },
  paymentChip: {
    borderWidth: 1.5,
    borderColor: colors.pillBorder,
    borderRadius: radius.pill,
    paddingHorizontal: spacing.md,
    paddingVertical: spacing.sm,
  },
  paymentChipSelected: {
    backgroundColor: colors.black,
    borderColor: colors.black,
  },
  paymentChipText: {
    fontSize: 14,
    color: colors.black,
  },
  paymentChipTextSelected: {
    color: colors.white,
  },
  error: {
    color: colors.red,
    marginTop: spacing.md,
    textAlign: 'center',
  },
  footer: {
    padding: spacing.lg,
    borderTopWidth: 1,
    borderTopColor: colors.pillBorder,
    ...centeredPage,
  },
});
