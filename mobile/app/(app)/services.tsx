import { Ionicons } from '@expo/vector-icons';
import { router, useFocusEffect } from 'expo-router';
import { useCallback, useState } from 'react';
import { ActivityIndicator, Pressable, ScrollView, StyleSheet, Text, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';

import { Button } from '../../components/Button';
import { Card } from '../../components/Card';
import { ConfirmDialog } from '../../components/ConfirmDialog';
import { Input } from '../../components/Input';
import { SectionHeader } from '../../components/SectionHeader';
import { useAuth } from '../../context/AuthContext';
import { ApiError } from '../../lib/api';
import { getBarber } from '../../lib/api/barbers';
import { getBarberShop } from '../../lib/api/barbershops';
import {
  createService,
  deleteService,
  listServices,
  toggleServiceAvailability,
  updateService,
} from '../../lib/api/services';
import { formatCurrency, formatDuration } from '../../lib/format';
import type { BarberShop, Service } from '../../lib/types';
import { colors } from '../../theme/colors';
import { centeredPage } from '../../theme/layout';
import { radius, spacing } from '../../theme/spacing';
import { typography } from '../../theme/typography';

interface FormState {
  id: number | null;
  name: string;
  description: string;
  durationMinutes: string;
  price: string;
}

const EMPTY_FORM: FormState = { id: null, name: '', description: '', durationMinutes: '', price: '' };

export default function ServicesScreen() {
  const { session } = useAuth();

  const [shop, setShop] = useState<BarberShop | null>(null);
  const [services, setServices] = useState<Service[]>([]);
  const [loading, setLoading] = useState(true);
  const [form, setForm] = useState<FormState | null>(null);
  const [saving, setSaving] = useState(false);
  const [pendingDelete, setPendingDelete] = useState<Service | null>(null);
  const [deleting, setDeleting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const isOwner = Boolean(shop && session && shop.ownerId === session.userId);

  const load = useCallback(async () => {
    if (!session) return;
    setLoading(true);
    setError(null);
    try {
      const me = await getBarber(session.userId);
      if (!me.barberShopId) {
        setShop(null);
        setServices([]);
        return;
      }
      const [shopData, servicesData] = await Promise.all([
        getBarberShop(me.barberShopId),
        listServices(me.barberShopId),
      ]);
      setShop(shopData);
      setServices(servicesData);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Não foi possível carregar os serviços.');
    } finally {
      setLoading(false);
    }
  }, [session]);

  useFocusEffect(
    useCallback(() => {
      load();
    }, [load]),
  );

  function canManage(service: Service) {
    return isOwner || service.barberId === session?.userId;
  }

  function openCreate() {
    setError(null);
    setForm({ ...EMPTY_FORM });
  }

  function openEdit(service: Service) {
    setError(null);
    setForm({
      id: service.id,
      name: service.name,
      description: service.description ?? '',
      durationMinutes: String(service.durationMinutes),
      price: String(service.price),
    });
  }

  async function handleSave() {
    if (!shop || !form) return;
    const durationMinutes = Number(form.durationMinutes);
    const price = Number(form.price.replace(',', '.'));
    if (!form.name.trim() || !durationMinutes || Number.isNaN(price)) {
      setError('Preencha nome, duração e preço corretamente.');
      return;
    }

    setSaving(true);
    setError(null);
    try {
      const payload = {
        name: form.name.trim(),
        description: form.description.trim() || undefined,
        durationMinutes,
        price,
      };
      if (form.id) {
        const updated = await updateService(shop.id, form.id, payload);
        setServices((prev) => prev.map((s) => (s.id === updated.id ? updated : s)));
      } else {
        const created = await createService(shop.id, payload);
        setServices((prev) => [...prev, created]);
      }
      setForm(null);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Não foi possível salvar o serviço.');
    } finally {
      setSaving(false);
    }
  }

  async function handleToggle(service: Service) {
    if (!shop) return;
    setError(null);
    try {
      const updated = await toggleServiceAvailability(shop.id, service.id);
      setServices((prev) => prev.map((s) => (s.id === updated.id ? updated : s)));
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Não foi possível atualizar.');
    }
  }

  async function handleDelete() {
    if (!shop || !pendingDelete) return;
    setDeleting(true);
    try {
      await deleteService(shop.id, pendingDelete.id);
      setServices((prev) => prev.filter((s) => s.id !== pendingDelete.id));
      setPendingDelete(null);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Não foi possível excluir.');
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
        <Text style={styles.headerTitle}>Serviços</Text>
        <View style={{ width: 24 }} />
      </View>

      <ScrollView contentContainerStyle={styles.content}>
        {error ? <Text style={styles.error}>{error}</Text> : null}

        {!shop ? (
          <View style={styles.empty}>
            <Ionicons name="cut-outline" size={40} color={colors.textFaint} />
            <Text style={styles.emptyText}>
              Você precisa fazer parte de uma barbearia para cadastrar serviços.
            </Text>
          </View>
        ) : (
          <>
            <SectionHeader
              title="Meus serviços"
              caption="Serviços que você (ou sua equipe) oferece nesta barbearia"
              action={
                !form ? (
                  <Pressable onPress={openCreate} hitSlop={8} style={styles.addButton}>
                    <Ionicons name="add" size={20} color={colors.white} />
                  </Pressable>
                ) : null
              }
            />

            {form ? (
              <Card style={styles.formCard}>
                <Input label="Nome" value={form.name} onChangeText={(v) => setForm({ ...form, name: v })} />
                <Input
                  label="Descrição"
                  value={form.description}
                  onChangeText={(v) => setForm({ ...form, description: v })}
                />
                <Input
                  label="Duração (min)"
                  keyboardType="number-pad"
                  value={form.durationMinutes}
                  onChangeText={(v) => setForm({ ...form, durationMinutes: v })}
                />
                <Input
                  label="Preço (R$)"
                  keyboardType="decimal-pad"
                  value={form.price}
                  onChangeText={(v) => setForm({ ...form, price: v })}
                />
                <View style={styles.formActions}>
                  <Button
                    title="Cancelar"
                    variant="outline"
                    size="sm"
                    onPress={() => setForm(null)}
                    style={styles.formButton}
                  />
                  <Button
                    title="Salvar"
                    size="sm"
                    loading={saving}
                    onPress={handleSave}
                    style={styles.formButton}
                  />
                </View>
              </Card>
            ) : null}

            {services.length === 0 && !form ? (
              <Text style={styles.emptyList}>Nenhum serviço cadastrado ainda.</Text>
            ) : (
              services.map((service) => {
                const manageable = canManage(service);
                return (
                  <Card key={service.id} style={styles.row} variant="flat">
                    <View style={styles.info}>
                      <Text style={styles.name}>{service.name}</Text>
                      <Text style={styles.meta}>
                        {formatDuration(service.durationMinutes)} · {formatCurrency(service.price)}
                      </Text>
                      {!manageable && service.barberName ? (
                        <Text style={styles.meta}>{service.barberName}</Text>
                      ) : null}
                    </View>

                    <Pressable
                      onPress={() => manageable && handleToggle(service)}
                      disabled={!manageable}
                      style={[
                        styles.tag,
                        { borderColor: service.available ? colors.success : colors.pillBorder },
                      ]}
                    >
                      <Text
                        style={[
                          styles.tagText,
                          { color: service.available ? colors.success : colors.textMuted },
                        ]}
                      >
                        {service.available ? 'Ativo' : 'Inativo'}
                      </Text>
                    </Pressable>

                    {manageable ? (
                      <View style={styles.actions}>
                        <Pressable onPress={() => openEdit(service)} hitSlop={8}>
                          <Ionicons name="pencil" size={18} color={colors.black} />
                        </Pressable>
                        <Pressable onPress={() => setPendingDelete(service)} hitSlop={8}>
                          <Ionicons name="trash-outline" size={18} color={colors.danger} />
                        </Pressable>
                      </View>
                    ) : null}
                  </Card>
                );
              })
            )}
          </>
        )}
      </ScrollView>

      <ConfirmDialog
        visible={pendingDelete !== null}
        title="Excluir serviço?"
        message={pendingDelete ? `"${pendingDelete.name}" será removido.` : undefined}
        confirmLabel="Excluir"
        destructive
        loading={deleting}
        onConfirm={handleDelete}
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
  empty: {
    alignItems: 'center',
    gap: spacing.md,
    marginTop: spacing.xxl,
    paddingHorizontal: spacing.lg,
  },
  emptyText: {
    color: colors.textMuted,
    textAlign: 'center',
    fontSize: 14,
    lineHeight: 20,
  },
  emptyList: {
    color: colors.textMuted,
    fontSize: 14,
  },
  addButton: {
    width: 32,
    height: 32,
    borderRadius: 16,
    backgroundColor: colors.blue,
    alignItems: 'center',
    justifyContent: 'center',
  },
  formCard: {
    marginBottom: spacing.md,
  },
  formActions: {
    flexDirection: 'row',
    gap: spacing.sm,
    marginTop: spacing.xs,
  },
  formButton: {
    flex: 1,
  },
  row: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: spacing.sm,
    marginBottom: spacing.sm,
  },
  info: {
    flex: 1,
  },
  name: {
    fontSize: 15,
    color: colors.black,
    fontWeight: '600',
  },
  meta: {
    fontSize: 12,
    color: colors.textMuted,
    marginTop: 2,
  },
  tag: {
    borderWidth: 1.5,
    borderRadius: radius.pill,
    paddingHorizontal: spacing.sm,
    paddingVertical: 4,
  },
  tagText: {
    fontSize: 11,
    fontWeight: '600',
    textTransform: 'uppercase',
    letterSpacing: 0.5,
  },
  actions: {
    flexDirection: 'row',
    gap: spacing.sm,
  },
});
