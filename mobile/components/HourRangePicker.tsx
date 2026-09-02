import { useState } from 'react';
import { Pressable, ScrollView, StyleSheet, Text, View } from 'react-native';

import { colors } from '../theme/colors';
import { fonts } from '../theme/typography';
import { radius, spacing } from '../theme/spacing';

interface HourRangePickerProps {
  from: number | null;
  to: number | null;
  min?: number;
  max?: number;
  onChange: (from: number, to: number) => void;
}

/**
 * Horizontal strip of hour chips. Tap once for "opens at", tap a later hour for
 * "closes at"; the span in between fills in. Works identically on web and native.
 */
export function HourRangePicker({ from, to, min = 6, max = 23, onChange }: HourRangePickerProps) {
  const [pendingStart, setPendingStart] = useState<number | null>(null);
  const hours = Array.from({ length: max - min + 1 }, (_, i) => min + i);

  function handlePress(hour: number) {
    if (pendingStart == null) {
      setPendingStart(hour);
      return;
    }
    if (hour > pendingStart) {
      onChange(pendingStart, hour);
      setPendingStart(null);
    } else {
      setPendingStart(hour);
    }
  }

  return (
    <View>
      <ScrollView horizontal showsHorizontalScrollIndicator={false} contentContainerStyle={styles.strip}>
        {hours.map((hour) => {
          const inRange = from != null && to != null && hour >= from && hour <= to;
          const isPending = pendingStart === hour;
          return (
            <Pressable
              key={hour}
              onPress={() => handlePress(hour)}
              style={[styles.chip, inRange && styles.chipInRange, isPending && styles.chipPending]}
            >
              <Text style={[styles.chipText, inRange && styles.chipTextInRange]}>
                {String(hour).padStart(2, '0')}h
              </Text>
            </Pressable>
          );
        })}
      </ScrollView>
      <Text style={styles.caption}>
        {pendingStart != null
          ? `Abre às ${String(pendingStart).padStart(2, '0')}h — toque no horário de fechamento`
          : from != null && to != null
            ? `${String(from).padStart(2, '0')}h às ${String(to).padStart(2, '0')}h`
            : 'Toque no horário de abertura'}
      </Text>
    </View>
  );
}

const styles = StyleSheet.create({
  strip: {
    gap: spacing.xs,
    paddingVertical: spacing.xs,
  },
  chip: {
    minWidth: 44,
    paddingVertical: spacing.sm,
    paddingHorizontal: spacing.sm,
    borderRadius: radius.sm,
    borderWidth: 1.5,
    borderColor: colors.pillBorder,
    backgroundColor: colors.surface,
    alignItems: 'center',
  },
  chipInRange: {
    backgroundColor: colors.black,
    borderColor: colors.black,
  },
  chipPending: {
    borderColor: colors.blue,
  },
  chipText: {
    fontFamily: fonts.headingMedium,
    fontSize: 13,
    color: colors.black,
  },
  chipTextInRange: {
    color: colors.white,
  },
  caption: {
    fontSize: 12,
    color: colors.textMuted,
    marginTop: 6,
  },
});
