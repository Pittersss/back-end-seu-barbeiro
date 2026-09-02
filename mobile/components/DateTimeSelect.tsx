import { useMemo } from 'react';
import { ActivityIndicator, Pressable, StyleSheet, Text, View } from 'react-native';

import type { OpenSlots } from '../lib/types';
import { colors } from '../theme/colors';
import { fonts } from '../theme/typography';
import { radius, spacing } from '../theme/spacing';
import { Calendar } from './Calendar';

interface DateTimeSelectProps {
  value: Date;
  onChange: (next: Date) => void;
  /** `YYYY-MM-DD` → bookable local-ISO start times, from the backend. */
  slotsByDate: OpenSlots;
  loading?: boolean;
}

export function dateKey(d: Date): string {
  const pad = (n: number) => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`;
}

function slotLabel(iso: string): string {
  const d = new Date(iso);
  const pad = (n: number) => String(n).padStart(2, '0');
  return `${pad(d.getHours())}:${pad(d.getMinutes())}`;
}

/**
 * Calendar + time-slot grid. The grid shows exactly the openings the backend
 * returned for the selected day (working window − break − blocks − booked − past,
 * each already sized to fit the chosen service).
 */
export function DateTimeSelect({ value, onChange, slotsByDate, loading }: DateTimeSelectProps) {
  const daySlots = useMemo(() => slotsByDate[dateKey(value)] ?? [], [slotsByDate, value]);
  const selectedIso = useMemo(() => {
    const pad = (n: number) => String(n).padStart(2, '0');
    return `${dateKey(value)}T${pad(value.getHours())}:${pad(value.getMinutes())}`;
  }, [value]);

  return (
    <View>
      <Calendar
        value={value}
        onChange={(day) => {
          const next = new Date(day);
          next.setHours(value.getHours(), value.getMinutes(), 0, 0);
          onChange(next);
        }}
        isDayDisabled={(day) => (slotsByDate[dateKey(day)]?.length ?? 0) === 0}
      />

      {loading ? (
        <ActivityIndicator style={styles.loading} color={colors.black} />
      ) : daySlots.length === 0 ? (
        <Text style={styles.empty}>Nenhum horário livre neste dia.</Text>
      ) : (
        <View style={styles.timeGrid}>
          {daySlots.map((iso) => {
            const selected = iso.startsWith(selectedIso);
            return (
              <Pressable
                key={iso}
                onPress={() => onChange(new Date(iso))}
                style={[styles.timePill, selected && styles.timePillSelected]}
              >
                <Text style={[styles.timeText, selected && styles.timeTextSelected]}>
                  {slotLabel(iso)}
                </Text>
              </Pressable>
            );
          })}
        </View>
      )}
    </View>
  );
}

const styles = StyleSheet.create({
  loading: {
    marginTop: spacing.lg,
  },
  empty: {
    marginTop: spacing.lg,
    color: colors.textMuted,
    fontSize: 14,
    textAlign: 'center',
  },
  timeGrid: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: spacing.sm,
    marginTop: spacing.md,
  },
  timePill: {
    paddingVertical: spacing.sm,
    paddingHorizontal: spacing.md,
    borderRadius: radius.pill,
    borderWidth: 1.5,
    borderColor: colors.pillBorder,
    backgroundColor: colors.surface,
  },
  timePillSelected: {
    backgroundColor: colors.blue,
    borderColor: colors.blue,
  },
  timeText: {
    fontSize: 14,
    color: colors.black,
    fontFamily: fonts.headingMedium,
  },
  timeTextSelected: {
    color: colors.white,
  },
});
