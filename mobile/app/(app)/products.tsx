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
import {
  createProduct,
  deleteProduct,
  listProducts,
  toggleProductAvailability,
  updateProduct,
} from '../../lib/api/products';
import { formatCurrency } from '../../lib/format';
import type { Product } from '../../lib/types';
import { colors } from '../../theme/colors';
import { centeredPage } from '../../theme/layout';
import { radius, spacing } from '../../theme/spacing';
import { typography } from '../../theme/typography';

interface FormState {
  id: number | null;
  name: string;
  description: string;
  price: string;
}

const EMPTY_FORM: FormState = { id: null, name: '', description: '', price: '' };

export default function ProductsScreen() {
  const { session } = useAuth();

  const [shopId, setShopId] = useState<number | null>(null);
  const [products, setProducts] = useState<Product[]>([]);
  const [loading, setLoading] = useState(true);
  const [form, setForm] = useState<FormState | null>(null);
  const [saving, setSaving] = useState(false);
  const [pendingDelete, setPendingDelete] = useState<Product | null>(null);
  const [deleting, setDeleting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(async () => {
    if (!session) return;
    setLoading(true);
    setError(null);
    try {
      const me = await getBarber(session.userId);
      if (!me.barberShopId) {
        setShopId(null);
        setProducts([]);
        return;
      }
      setShopId(me.barberShopId);
      setProducts(await listProducts(me.barberShopId));
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Não foi possível carregar os produtos.');
    } finally {
      setLoading(false);
    }
  }, [session]);

  useFocusEffect(
    useCallback(() => {
      load();
    }, [load]),
  );

  function openCreate() {
    setError(null);
    setForm({ ...EMPTY_FORM });
  }

  function openEdit(product: Product) {
    setError(null);
    setForm({
      id: product.id,
      name: product.name,
      description: product.description ?? '',
      price: String(product.price),
    });
  }

  async function handleSave() {
    if (!shopId || !form) return;
    const price = Number(form.price.replace(',', '.'));
    if (!form.name.trim() || Number.isNaN(price)) {
      setError('Preencha nome e preço corretamente.');
      return;
    }

    setSaving(true);
    setError(null);
    try {
      const payload = {
        name: form.name.trim(),
        description: form.description.trim() || undefined,
        price,
      };
      if (form.id) {
        const updated = await updateProduct(shopId, form.id, payload);
        setProducts((prev) => prev.map((p) => (p.id === updated.id ? updated : p)));
      } else {
        const created = await createProduct(shopId, payload);
        setProducts((prev) => [...prev, created]);
      }
      setForm(null);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Não foi possível salvar o produto.');
    } finally {
      setSaving(false);
    }
  }

  async function handleToggle(product: Product) {
    if (!shopId) return;
    setError(null);
    try {
      const updated = await toggleProductAvailability(shopId, product.id);
      setProducts((prev) => prev.map((p) => (p.id === updated.id ? updated : p)));
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Não foi possível atualizar.');
    }
  }

  async function handleDelete() {
    if (!shopId || !pendingDelete) return;
    setDeleting(true);
    try {
      await deleteProduct(shopId, pendingDelete.id);
      setProducts((prev) => prev.filter((p) => p.id !== pendingDelete.id));
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
        <Text style={styles.headerTitle}>Produtos</Text>
        <View style={{ width: 24 }} />
      </View>

      <ScrollView contentContainerStyle={styles.content}>
        {error ? <Text style={styles.error}>{error}</Text> : null}

        {!shopId ? (
          <View style={styles.empty}>
            <Ionicons name="pricetag-outline" size={40} color={colors.textFaint} />
            <Text style={styles.emptyText}>
              Você precisa ser dono de uma barbearia aprovada para cadastrar produtos.
            </Text>
          </View>
        ) : (
          <>
            <SectionHeader
              title="Produtos à venda"
              caption="Itens vendidos na sua barbearia"
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

            {products.length === 0 && !form ? (
              <Text style={styles.emptyList}>Nenhum produto cadastrado ainda.</Text>
            ) : (
              products.map((product) => (
                <Card key={product.id} style={styles.row} variant="flat">
                  <View style={styles.info}>
                    <Text style={styles.name}>{product.name}</Text>
                    <Text style={styles.meta}>{formatCurrency(product.price)}</Text>
                  </View>

                  <Pressable
                    onPress={() => handleToggle(product)}
                    style={[
                      styles.tag,
                      { borderColor: product.available ? colors.success : colors.pillBorder },
                    ]}
                  >
                    <Text
                      style={[
                        styles.tagText,
                        { color: product.available ? colors.success : colors.textMuted },
                      ]}
                    >
                      {product.available ? 'Ativo' : 'Inativo'}
                    </Text>
                  </Pressable>

                  <View style={styles.actions}>
                    <Pressable onPress={() => openEdit(product)} hitSlop={8}>
                      <Ionicons name="pencil" size={18} color={colors.black} />
                    </Pressable>
                    <Pressable onPress={() => setPendingDelete(product)} hitSlop={8}>
                      <Ionicons name="trash-outline" size={18} color={colors.danger} />
                    </Pressable>
                  </View>
                </Card>
              ))
            )}
          </>
        )}
      </ScrollView>

      <ConfirmDialog
        visible={pendingDelete !== null}
        title="Excluir produto?"
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
