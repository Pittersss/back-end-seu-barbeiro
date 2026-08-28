import { request } from '../api';
import type { Barber, BarberPostPut } from '../types';

export function getBarber(id: number) {
  return request<Barber>(`/api/barbers/${id}`);
}

export function updateBarber(id: number, payload: BarberPostPut) {
  return request<Barber>(`/api/barbers/${id}`, { method: 'PUT', body: payload });
}
