import { request } from '../api';
import type { Product, ProductPostPut } from '../types';

export function listProducts(shopId: number) {
  return request<Product[]>(`/api/barbershops/${shopId}/products`);
}

export function createProduct(shopId: number, payload: ProductPostPut) {
  return request<Product>(`/api/barbershops/${shopId}/products`, {
    method: 'POST',
    body: payload,
  });
}

export function updateProduct(shopId: number, productId: number, payload: ProductPostPut) {
  return request<Product>(`/api/barbershops/${shopId}/products/${productId}`, {
    method: 'PUT',
    body: payload,
  });
}

export function deleteProduct(shopId: number, productId: number) {
  return request<void>(`/api/barbershops/${shopId}/products/${productId}`, {
    method: 'DELETE',
  });
}

export function toggleProductAvailability(shopId: number, productId: number) {
  return request<Product>(`/api/barbershops/${shopId}/products/${productId}/availability`, {
    method: 'PATCH',
  });
}
