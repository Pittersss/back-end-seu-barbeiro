import { Ionicons } from '@expo/vector-icons';
import { router, useFocusEffect } from 'expo-router';
import { useCallback, useState } from 'react';
import {
  ActivityIndicator,
  Pressable,
  ScrollView,
  StyleSheet,
  Switch,
  Text,
  View,
} from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';

import { Button } from '../../components/Button';
import { Calendar } from '../../components/Calendar';
import { Card } from '../../components/Card';
import { ConfirmDialog } from '../../components/ConfirmDialog';
import { HourRangePicker } from '../../components/HourRangePicker';
import { Input } from '../../components/Input';
import { SectionHeader } from '../../components/SectionHeader';
import { useAuth } from '../../context/AuthContext';
import { ApiError } from '../../lib/api';
import {
  createTimeBlock,
  deleteTimeBlock,
  getBarber,
  listTimeBlocks,
  toggleBarberAvailability,
  updateBarber,
} from '../../lib/api/barbers';
import { formatDate, formatTime, toLocalIso } from '../../lib/format';
import type { Barber, TimeBlock } from '../../lib/types';
import { colors } from '../../theme/colors';
import { centeredPage } from '../../theme/layout';
import { radius, spacing } from '../../theme/spacing';
import { typography } from '../../theme/typography';

function tomorrow(): Date {
  const d = new Date();
  d.setDate(d.getDate() + 1);
  d.setHours(0, 0, 0, 0);
  return d;
}

export default function AvailabilityScreen() {
  const { session } = useAuth();
  const barberId = session?.userId;

  const [barber, setBarber] = useState<Barber | null>(null);
  const [blocks, setBlocks] = useState<TimeBlock[]>([]);
  const [loading, setLoading] = useState(true);

  const [workFrom, setWorkFrom] = useState<number | null>(null);
  const [workTo, setWorkTo] = useState<number | null>(null);
  const [hasBreak, setHasBreak] = useState(false);
  const [breakFrom, setBreakFrom] = useState<number | null>(null);
  const [breakTo, setBreakTo] = useState<number | null>(null);
  const [savingHours, setSavingHours] = useState(false);

  const [togglingAvailable, setTogglingAvailable] = useState(false);

  const [showBlockForm, setShowBlockForm] = useState(false);
  const [blockDay, setBlockDay] = useState<Date>(tomorrow);
  const [allDay, setAllDay] = useState(true);
  const [blockFrom, setBlockFrom] = useState<number | null>(9);
  const [blockTo, setBlockTo] = useState<number | null>(12);
  const [blockReason, setBlockReason] = useState('');
  const [savingBlock, setSavingBlock] = useState(false);
  const [pendingDelete, setPendingDelete] = useState<TimeBlock | null>(null);
  const [deleting, setDeleting] = useState(false);

  const [error, setError] = useState<string | null>(null);
  const [saved, setSaved] = useState<string | null>(null);

  const flash = useCallback((msg: string) => {
    setSaved(msg);
    setTimeout(() => setSaved(null), 2000);
  }, []);

  const load = useCallback(async () => {
    if (!barberId) return;
    try {
      const [b, tb] = await Promise.all([getBarber(barberId), listTimeBlocks(barberId)]);
      setBarber(b);
      setWorkFrom(b.workStartHour);
      setWorkTo(b.workEndHour);
      setHasBreak(b.breakStartHour != null && b.breakEndHour != null);
      setBreakFrom(b.breakStartHour);
      setBreakTo(b.breakEndHour);
      setBlocks(tb);
    } catch {
      // 401s are handled by AuthContext.
    } finally {
      setLoading(false);
    }
  }, [barberId]);

  useFocusEffect(
    useCallback(() => {
      load();
    }, [load]),
  );

  async function handleToggleAvailable() {
    if (!barberId) return;
    setTogglingAvailable(true);
    try {
      setBarber(await toggleBarberAvailability(barberId));
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Não foi possível alterar.');
    } finally {
      setTogglingAvailable(false);
    }
  }

  async function handleSaveHours() {
    if (!barber || !barberId || workFrom == null || workTo == null) return;
    setError(null);
    setSavingHours(true);
    try {
      const updated = await updateBarber(barberId, {
        name: barber.name,
        phone: barber.phone,
        pixKey: barber.pixKey,
        delayTolerance: barber.delayTolerance,
        workStartHour: workFrom,
        workEndHour: workTo,
        breakStartHour: hasBreak ? breakFrom : null,
        breakEndHour: hasBreak ? breakTo : null,
      });
      setBarber(updated);
      flash('Horário salvo');
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Não foi possível salvar.');
    } finally {
      setSavingHours(false);
    }
  }

  async function handleAddBlock() {
    if (!barberId) return;
    const start = new Date(blockDay);
    const end = new Date(blockDay);
    if (allDay) {
      start.setHours(0, 0, 0, 0);
      end.setHours(23, 59, 0, 0);
    } else {
      if (blockFrom == null || blockTo == null) return;
      start.setHours(blockFrom, 0, 0, 0);
      end.setHours(blockTo, 0, 0, 0);
    }
    setError(null);
    setSavingBlock(true);
    try {
      await createTimeBlock(barberId, {
        startsAt: toLocalIso(start),
        endsAt: toLocalIso(end),
        reason: blockReason.trim() || null,
      });
      setShowBlockForm(false);
      setBlockReason('');
      await load();
      flash('Bloqueio adicionado');
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Não foi possível adicionar.');
    } finally {
      setSavingBlock(false);
    }
  }

  async function handleDeleteBlock() {
    if (!barberId || !pendingDelete) return;
    setDeleting(true);
    try {
      await deleteTimeBlock(barberId, pendingDelete.id);
      setBlocks((prev) => prev.filter((b) => b.id !== pendingDelete.id));
      setPendingDelete(null);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Não foi possível remover.');
    } finally {
      setDeleting(false);
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
        <Text style={styles.headerTitle}>Disponibilidade</Text>
        <View style={{ width: 24 }} />
      </View>

      <ScrollView contentContainerStyle={styles.content}>
        {error ? <Text style={styles.error}>{error}</Text> : null}
        {saved ? <Text style={styles.saved}>{saved}</Text> : null}

        <Card style={styles.rowCard}>
          <View style={styles.rowText}>
            <Text style={styles.rowTitle}>Disponível para agendamentos</Text>
            <Text style={styles.rowHint}>
              Desligado, você não aparece para novos clientes.
            </Text>
          </View>
          <Switch
            value={barber?.available ?? false}
            onValueChange={handleToggleAvailable}
            disabled={togglingAvailable}
            trackColor={{ false: colors.pillBorder, true: colors.blue }}
          />
        </Card>

        <SectionHeader title="Horário de atendimento" caption="Vale para todos os dias" />
        <Card>
          <HourRangePicker
            from={workFrom}
            to={workTo}
            onChange={(f, t) => {
              setWorkFrom(f);
              setWorkTo(t);
            }}
          />

          <View style={styles.breakToggleRow}>
            <Text style={styles.rowTitle}>Tenho um intervalo</Text>
            <Switch
              value={hasBreak}
              onValueChange={(v) => {
                setHasBreak(v);
                if (v && (breakFrom == null || breakTo == null)) {
                  setBreakFrom(12);
                  setBreakTo(13);
                }
              }}
              trackColor={{ false: colors.pillBorder, true: colors.blue }}
            />
          </View>
          {hasBreak ? (
            <HourRangePicker
              from={breakFrom}
              to={breakTo}
              min={workFrom ?? 6}
              max={workTo ?? 23}
              onChange={(f, t) => {
                setBreakFrom(f);
                setBreakTo(t);
              }}
            />
          ) : null}

          <Button
            title="Salvar horário"
            size="sm"
            loading={savingHours}
            onPress={handleSaveHours}
            style={styles.saveHours}
          />
        </Card>

        <SectionHeader
          title="Folgas e bloqueios"
          caption="Datas específicas em que você não atende"
          action={
            <Pressable onPress={() => setShowBlockForm((v) => !v)} hitSlop={8}>
              <Ionicons
                name={showBlockForm ? 'close' : 'add'}
                size={22}
                color={colors.blue}
              />
            </Pressable>
          }
        />

        {showBlockForm ? (
          <Card style={styles.formCard}>
            <Calendar value={blockDay} onChange={setBlockDay} />
            <View style={styles.breakToggleRow}>
              <Text style={styles.rowTitle}>Dia inteiro</Text>
              <Switch
                value={allDay}
                onValueChange={setAllDay}
                trackColor={{ false: colors.pillBorder, true: colors.blue }}
              />
            </View>
            {!allDay ? (
              <HourRangePicker
                from={blockFrom}
                to={blockTo}
                onChange={(f, t) => {
                  setBlockFrom(f);
                  setBlockTo(t);
                }}
              />
            ) : null}
            <Input
              label="Motivo (opcional)"
              placeholder="Ex.: consulta médica"
              value={blockReason}
              onChangeText={setBlockReason}
            />
            <Button
              title="Adicionar bloqueio"
              size="sm"
              loading={savingBlock}
              onPress={handleAddBlock}
            />
          </Card>
        ) : null}

        {blocks.length === 0 ? (
          <Text style={styles.emptyBlocks}>Nenhum bloqueio ativo.</Text>
        ) : (
          blocks.map((block) => (
            <Card key={block.id} style={styles.blockRow} variant="flat">
              <View style={styles.blockInfo}>
                <Text style={styles.blockDate}>{formatDate(block.startsAt)}</Text>
                <Text style={styles.blockTime}>
                  {formatTime(block.startsAt)} – {formatTime(block.endsAt)}
                  {block.reason ? ` · ${block.reason}` : ''}
                </Text>
              </View>
              <Pressable onPress={() => setPendingDelete(block)} hitSlop={8}>
                <Text style={styles.remove}>Remover</Text>
              </Pressable>
            </Card>
          ))
        )}
      </ScrollView>

      <ConfirmDialog
        visible={pendingDelete !== null}
        title="Remover bloqueio?"
        message="Os clientes voltarão a poder agendar nesse período."
        confirmLabel="Remover"
        destructive
        loading={deleting}
        onConfirm={handleDeleteBlock}
        onCancel={() => setPendingDelete(null)}
      />
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
  error: {
    color: colors.red,
    textAlign: 'center',
    marginBottom: spacing.sm,
  },
  saved: {
    color: colors.success,
    textAlign: 'center',
    marginBottom: spacing.sm,
  },
  rowCard: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: spacing.md,
  },
  rowText: {
    flex: 1,
  },
  rowTitle: {
    fontSize: 15,
    color: colors.black,
    fontWeight: '600',
  },
  rowHint: {
    fontSize: 12,
    color: colors.textMuted,
    marginTop: 2,
  },
  breakToggleRow: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    marginTop: spacing.md,
  },
  saveHours: {
    marginTop: spacing.md,
    alignSelf: 'flex-start',
    paddingHorizontal: spacing.lg,
  },
  formCard: {
    gap: spacing.sm,
    marginBottom: spacing.md,
  },
  emptyBlocks: {
    color: colors.textMuted,
    fontSize: 14,
    marginTop: spacing.sm,
  },
  blockRow: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    marginBottom: spacing.sm,
  },
  blockInfo: {
    flex: 1,
  },
  blockDate: {
    fontSize: 14,
    color: colors.black,
    fontWeight: '600',
    textTransform: 'capitalize',
  },
  blockTime: {
    fontSize: 12,
    color: colors.textMuted,
    marginTop: 2,
  },
  remove: {
    color: colors.danger,
    fontSize: 13,
    fontWeight: '600',
  },
});
