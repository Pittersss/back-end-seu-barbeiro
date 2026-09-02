import { Ionicons } from '@expo/vector-icons';
import { useMemo, useState } from 'react';
import { Pressable, StyleSheet, Text, View } from 'react-native';

import { colors } from '../theme/colors';
import { fonts, typography } from '../theme/typography';
import { radius, spacing } from '../theme/spacing';

interface CalendarProps {
  value: Date;
  onChange: (next: Date) => void;
  minDate?: Date;
  isDayDisabled?: (day: Date) => boolean;
}

const WEEKDAYS = ['D', 'S', 'T', 'Q', 'Q', 'S', 'S'];
const MONTHS = [
  'Janeiro', 'Fevereiro', 'Março', 'Abril', 'Maio', 'Junho',
  'Julho', 'Agosto', 'Setembro', 'Outubro', 'Novembro', 'Dezembro',
];

function startOfDay(d: Date): Date {
  const next = new Date(d);
  next.setHours(0, 0, 0, 0);
  return next;
}

function sameDay(a: Date, b: Date): boolean {
  return (
    a.getFullYear() === b.getFullYear() &&
    a.getMonth() === b.getMonth() &&
    a.getDate() === b.getDate()
  );
}

/**
 * Month-grid date picker built only from RN primitives —
 * `@react-native-community/datetimepicker` renders nothing on the Expo web
 * target, so scheduling UI can't rely on it.
 */
export function Calendar({ value, onChange, minDate, isDayDisabled }: CalendarProps) {
  const floor = useMemo(() => startOfDay(minDate ?? new Date()), [minDate]);
  const [viewMonth, setViewMonth] = useState(
    () => new Date(value.getFullYear(), value.getMonth(), 1),
  );
  const today = startOfDay(new Date());

  const cells = useMemo(() => {
    const firstWeekday = viewMonth.getDay();
    const daysInMonth = new Date(
      viewMonth.getFullYear(),
      viewMonth.getMonth() + 1,
      0,
    ).getDate();
    const out: (Date | null)[] = [];
    for (let i = 0; i < firstWeekday; i += 1) out.push(null);
    for (let d = 1; d <= daysInMonth; d += 1) {
      out.push(new Date(viewMonth.getFullYear(), viewMonth.getMonth(), d));
    }
    while (out.length % 7 !== 0) out.push(null);
    return out;
  }, [viewMonth]);

  const canGoBack =
    viewMonth.getFullYear() > floor.getFullYear() ||
    (viewMonth.getFullYear() === floor.getFullYear() &&
      viewMonth.getMonth() > floor.getMonth());

  function shiftMonth(delta: number) {
    setViewMonth((m) => new Date(m.getFullYear(), m.getMonth() + delta, 1));
  }

  return (
    <View style={styles.wrap}>
      <View style={styles.header}>
        <Pressable
          onPress={() => canGoBack && shiftMonth(-1)}
          disabled={!canGoBack}
          hitSlop={10}
          style={styles.navBtn}
        >
          <Ionicons
            name="chevron-back"
            size={20}
            color={canGoBack ? colors.black : colors.textFaint}
          />
        </Pressable>
        <Text style={styles.monthLabel}>
          {MONTHS[viewMonth.getMonth()]} {viewMonth.getFullYear()}
        </Text>
        <Pressable onPress={() => shiftMonth(1)} hitSlop={10} style={styles.navBtn}>
          <Ionicons name="chevron-forward" size={20} color={colors.black} />
        </Pressable>
      </View>

      <View style={styles.weekRow}>
        {WEEKDAYS.map((w, i) => (
          <Text key={i} style={styles.weekday}>
            {w}
          </Text>
        ))}
      </View>

      <View style={styles.grid}>
        {cells.map((day, i) => {
          if (!day) return <View key={i} style={styles.cell} />;
          const disabled =
            day < floor || (isDayDisabled ? isDayDisabled(day) : false);
          const selected = sameDay(day, value);
          const isToday = sameDay(day, today);
          return (
            <View key={i} style={styles.cell}>
              <Pressable
                onPress={() => !disabled && onChange(day)}
                disabled={disabled}
                style={[
                  styles.day,
                  selected && styles.daySelected,
                  !selected && isToday && styles.dayToday,
                ]}
              >
                <Text
                  style={[
                    styles.dayText,
                    selected && styles.dayTextSelected,
                    disabled && styles.dayTextDisabled,
                  ]}
                >
                  {day.getDate()}
                </Text>
              </Pressable>
            </View>
          );
        })}
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  wrap: {
    backgroundColor: colors.surface,
    borderRadius: radius.card,
    borderWidth: 1,
    borderColor: colors.line,
    padding: spacing.md,
  },
  header: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    marginBottom: spacing.sm,
  },
  navBtn: {
    width: 32,
    height: 32,
    alignItems: 'center',
    justifyContent: 'center',
  },
  monthLabel: {
    ...typography.h3,
    color: colors.black,
  },
  weekRow: {
    flexDirection: 'row',
  },
  weekday: {
    flex: 1,
    textAlign: 'center',
    ...typography.caption,
    color: colors.textFaint,
    textTransform: 'uppercase',
    marginBottom: 4,
  },
  grid: {
    flexDirection: 'row',
    flexWrap: 'wrap',
  },
  cell: {
    width: `${100 / 7}%`,
    aspectRatio: 1,
    alignItems: 'center',
    justifyContent: 'center',
    padding: 2,
  },
  day: {
    width: '100%',
    height: '100%',
    maxWidth: 40,
    maxHeight: 40,
    borderRadius: radius.pill,
    alignItems: 'center',
    justifyContent: 'center',
  },
  daySelected: {
    backgroundColor: colors.black,
  },
  dayToday: {
    borderWidth: 1.5,
    borderColor: colors.pillBorder,
  },
  dayText: {
    fontFamily: fonts.headingMedium,
    fontSize: 14,
    color: colors.black,
  },
  dayTextSelected: {
    color: colors.white,
  },
  dayTextDisabled: {
    color: colors.textFaint,
  },
});
