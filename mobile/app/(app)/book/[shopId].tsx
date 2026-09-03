import { Ionicons } from '@expo/vector-icons';
import { router, useFocusEffect, useLocalSearchParams } from 'expo-router';
import { useCallback, useEffect, useMemo, useState } from 'react';
import { ActivityIndicator, Pressable, ScrollView, StyleSheet, Text, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';

import { Avatar } from '../../../components/Avatar';
import { Button } from '../../../components/Button';
import { Card } from '../../../components/Card';
import { DateTimeSelect, dateKey } from '../../../components/DateTimeSelect';
import { SectionHeader } from '../../../components/SectionHeader';
import { ApiError } from '../../../lib/api';
import { createAppointment } from '../../../lib/api/appointments';
import { getOpenSlots } from '../../../lib/api/barbers';
import { listShopBarbers } from '../../../lib/api/barbershops';
import { listServices } from '../../../lib/api/services';
import { formatCurrency, formatDate, formatDuration, formatTime, toLocalIso } from '../../../lib/format';
import type { Barber, OpenSlots, PaymentMethod, Service } from '../../../lib/types';
import { colors } from '../../../theme/colors';
import { centeredPage } from '../../../theme/layout';
import { radius, spacing } from '../../../theme/spacing';
import { typography } from '../../../theme/typography';

const PAYMENT_OPTIONS: { value: PaymentMethod; label: string; icon: keyof typeof Ionicons.glyphMap }[] = [
  { value: 'PIX', label: 'Pix', icon: 'qr-code-outline' },
  { value: 'CARD', label: 'Cartão', icon: 'card-outline' },
  { value: 'CASH', label: 'Dinheiro', icon: 'cash-outline' },
];

const BOOKING_WINDOW_DAYS = 31;

function firstSlot(slots: OpenSlots): Date | null {
  const days = Object.keys(slots).sort();
  for (const day of days) {
    if (slots[day]?.length) return new Date(slots[day][0]);
  }
  return null;
}

function isKnownSlot(slots: OpenSlots, when: Date): boolean {
  return (slots[dateKey(when)] ?? []).some((iso) => {
    const d = new Date(iso);
    return d.getHours() === when.getHours() && d.getMinutes() === when.getMinutes();
  });
}

export default function BookingScreen() {
  const { shopId } = useLocalSearchParams<{ shopId: string }>();
  const shopIdNumber = Number(shopId);

  const [services, setServices] = useState<Service[]>([]);
  const [barbers, setBarbers] = useState<Barber[]>([]);
  const [loading, setLoading] = useState(true);

  const [service, setService] = useState<Service | null>(null);
  const [barber, setBarber] = useState<Barber | null>(null);
  const [scheduledAt, setScheduledAt] = useState<Date | null>(null);
  const [slotsByDate, setSlotsByDate] = useState<OpenSlots>({});
  const [slotsLoading, setSlotsLoading] = useState(false);
  const [paymentMethod, setPaymentMethod] = useState<PaymentMethod>('PIX');
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useFocusEffect(
    useCallback(() => {
      let cancelled = false;
      (async () => {
        setLoading(true);
        const [servicesData, barbersData] = await Promise.all([
          listServices(shopIdNumber),
          listShopBarbers(shopIdNumber),
        ]);
        if (cancelled) return;
        setServices(servicesData.filter((s) => s.available));
        setBarbers(barbersData.filter((b) => b.available));
        setLoading(false);
      })();
      return () => {
        cancelled = true;
      };
    }, [shopIdNumber]),
  );

  useEffect(() => {
    if (!service || !barber) {
      setSlotsByDate({});
      setScheduledAt(null);
      return;
    }
    let cancelled = false;
    setSlotsLoading(true);
    const today = new Date();
    const to = new Date();
    to.setDate(today.getDate() + BOOKING_WINDOW_DAYS);
    getOpenSlots(barber.id, service.id, dateKey(today), dateKey(to))
      .then((slots) => {
        if (cancelled) return;
        setSlotsByDate(slots);
        setScheduledAt((current) =>
          current && isKnownSlot(slots, current) ? current : firstSlot(slots),
        );
      })
      .catch(() => {
        if (!cancelled) setSlotsByDate({});
      })
      .finally(() => {
        if (!cancelled) setSlotsLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, [service, barber]);

  const visibleServices = useMemo(
    () => (barber ? services.filter((s) => s.barberId == null || s.barberId === barber.id) : services),
    [services, barber],
  );

  // A service either belongs to one specific barber or (barberId == null) is
  // shared by the whole team, so this is a single O(n) pass over barbers —
  // no per-barber lookups or extra requests.
  const visibleBarbers = useMemo(
    () => (service?.barberId != null ? barbers.filter((b) => b.id === service.barberId) : barbers),
    [barbers, service],
  );

  function handleSelectService(item: Service) {
    setService(item);
    setBarber((current) =>
      current && item.barberId != null && current.id !== item.barberId ? null : current,
    );
  }

  function handleSelectBarber(item: Barber) {
    setBarber(item);
    setService((current) =>
      current && current.barberId != null && current.barberId !== item.id ? null : current,
    );
  }

  const canSubmit = Boolean(
    service && barber && scheduledAt && isKnownSlot(slotsByDate, scheduledAt),
  );

  const summary = useMemo(() => {
    if (!service) return 'Selecione um serviço';
    if (!scheduledAt) return `${service.name} · escolha um horário`;
    return `${service.name} · ${formatDate(scheduledAt.toISOString())} · ${formatTime(scheduledAt.toISOString())}`;
  }, [service, scheduledAt]);

  async function handleSubmit() {
    if (!service || !barber || !scheduledAt) return;
    setError(null);
    setSubmitting(true);
    try {
      const appointment = await createAppointment({
        barberId: barber.id,
        serviceId: service.id,
        scheduledAt: toLocalIso(scheduledAt),
        paymentMethod,
      });
      if (paymentMethod === 'PIX') {
        router.replace({
          pathname: '/(app)/appointment/[id]/pix',
          params: { id: String(appointment.id) },
        });
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
        <Text style={styles.headerTitle}>Agendar horário</Text>
        <View style={{ width: 24 }} />
      </View>

      <ScrollView contentContainerStyle={styles.content}>
        <SectionHeader title="Serviço" style={styles.firstSection} />
        {barber && visibleServices.length === 0 ? (
          <Text style={styles.hint}>{barber.name} não tem serviços cadastrados.</Text>
        ) : (
          visibleServices.map((item) => {
            const selected = service?.id === item.id;
            return (
              <Pressable key={item.id} onPress={() => handleSelectService(item)}>
                <Card style={[styles.optionCard, selected && styles.optionCardSelected]} variant="flat">
                  <View style={styles.serviceInfo}>
                    <Text style={styles.optionTitle}>{item.name}</Text>
                    <Text style={styles.optionMeta}>{formatDuration(item.durationMinutes)}</Text>
                  </View>
                  <Text style={styles.optionPrice}>{formatCurrency(item.price)}</Text>
                </Card>
              </Pressable>
            );
          })
        )}

        <SectionHeader title="Barbeiro" />
        <View style={styles.barbersRow}>
          {visibleBarbers.map((item) => {
            const selected = barber?.id === item.id;
            return (
              <Pressable key={item.id} style={styles.barberChip} onPress={() => handleSelectBarber(item)}>
                <Avatar
                  name={item.name}
                  avatarBase64={item.avatarBase64}
                  size={56}
                  tone={selected ? 'black' : 'blue'}
                  style={selected ? styles.barberAvatarSelected : undefined}
                />
                <Text style={[styles.barberName, selected && styles.barberNameSelected]} numberOfLines={1}>
                  {item.name}
                </Text>
              </Pressable>
            );
          })}
        </View>

        <SectionHeader title="Data e hora" />
        {!service || !barber ? (
          <Text style={styles.hint}>Escolha o serviço e o barbeiro para ver os horários livres.</Text>
        ) : (
          <DateTimeSelect
            value={scheduledAt ?? new Date()}
            onChange={setScheduledAt}
            slotsByDate={slotsByDate}
            loading={slotsLoading}
          />
        )}

        <SectionHeader title="Pagamento" />
        <View style={styles.paymentRow}>
          {PAYMENT_OPTIONS.map((option) => {
            const selected = paymentMethod === option.value;
            return (
              <Pressable
                key={option.value}
                style={[styles.paymentChip, selected && styles.paymentChipSelected]}
                onPress={() => setPaymentMethod(option.value)}
              >
                <Ionicons name={option.icon} size={16} color={selected ? colors.white : colors.black} />
                <Text style={[styles.paymentChipText, selected && styles.paymentChipTextSelected]}>
                  {option.label}
                </Text>
              </Pressable>
            );
          })}
        </View>

        {error ? <Text style={styles.error}>{error}</Text> : null}
      </ScrollView>

      <View style={styles.footer}>
        <View style={styles.summaryRow}>
          <Text style={styles.summaryText} numberOfLines={1}>
            {summary}
          </Text>
          {service ? <Text style={styles.summaryPrice}>{formatCurrency(service.price)}</Text> : null}
        </View>
        <Button
          title="Confirmar agendamento"
          loading={submitting}
          disabled={!canSubmit}
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
  firstSection: {
    marginTop: spacing.sm,
  },
  optionCard: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: spacing.sm,
  },
  optionCardSelected: {
    borderColor: colors.black,
    backgroundColor: colors.surfaceAlt,
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
  barberAvatarSelected: {
    borderWidth: 2,
    borderColor: colors.black,
  },
  barberName: {
    fontSize: 12,
    color: colors.black,
    textAlign: 'center',
    marginTop: 4,
  },
  barberNameSelected: {
    fontFamily: typography.label.fontFamily,
  },
  hint: {
    fontSize: 13,
    color: colors.textMuted,
  },
  paymentRow: {
    flexDirection: 'row',
    gap: spacing.sm,
  },
  paymentChip: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 6,
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
    borderTopColor: colors.line,
    backgroundColor: colors.surface,
    ...centeredPage,
  },
  summaryRow: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    gap: spacing.sm,
    marginBottom: spacing.sm,
  },
  summaryText: {
    flex: 1,
    fontSize: 13,
    color: colors.textMuted,
  },
  summaryPrice: {
    fontFamily: typography.h2.fontFamily,
    fontSize: 15,
    color: colors.black,
  },
});
