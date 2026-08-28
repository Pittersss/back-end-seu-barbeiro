import { request } from '../api';
import type {
  Barber,
  BarberShop,
  BarberShopPostPut,
  BarberShopRequestPayload,
  BarberShopRequestResponse,
} from '../types';

export function listBarberShops() {
  return request<BarberShop[]>('/api/barbershops');
}

export function getBarberShop(id: number) {
  return request<BarberShop>(`/api/barbershops/${id}`);
}

export function requestBarberShopCreation(payload: BarberShopRequestPayload) {
  return request<BarberShopRequestResponse>('/api/barbershops', { method: 'POST', body: payload });
}

export function updateBarberShop(id: number, payload: BarberShopPostPut) {
  return request<BarberShop>(`/api/barbershops/${id}`, { method: 'PUT', body: payload });
}

export function toggleAcceptingBarbers(id: number) {
  return request<BarberShop>(`/api/barbershops/${id}/accepting-barbers`, { method: 'PATCH' });
}

export function listShopBarbers(shopId: number) {
  return request<Barber[]>(`/api/barbershops/${shopId}/barbers`);
}
