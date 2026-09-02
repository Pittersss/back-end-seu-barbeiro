import { request } from '../api';
import type {
  Barber,
  BarberPostPut,
  BlockClientPost,
  BlockedClient,
  OpenSlots,
  TimeBlock,
  TimeBlockPost,
} from '../types';

export function getBarber(id: number) {
  return request<Barber>(`/api/barbers/${id}`);
}

export function updateBarber(id: number, payload: BarberPostPut) {
  return request<Barber>(`/api/barbers/${id}`, { method: 'PUT', body: payload });
}

export function toggleBarberAvailability(id: number) {
  return request<Barber>(`/api/barbers/${id}/availability`, { method: 'PATCH' });
}

export function listTimeBlocks(barberId: number) {
  return request<TimeBlock[]>(`/api/barbers/${barberId}/time-blocks`);
}

export function createTimeBlock(barberId: number, payload: TimeBlockPost) {
  return request<TimeBlock>(`/api/barbers/${barberId}/time-blocks`, {
    method: 'POST',
    body: payload,
  });
}

export function deleteTimeBlock(barberId: number, blockId: number) {
  return request<void>(`/api/barbers/${barberId}/time-blocks/${blockId}`, {
    method: 'DELETE',
  });
}

export function listBlockedClients(barberId: number) {
  return request<BlockedClient[]>(`/api/barbers/${barberId}/blocked-clients`);
}

export function blockClient(barberId: number, payload: BlockClientPost) {
  return request<BlockedClient>(`/api/barbers/${barberId}/blocked-clients`, {
    method: 'POST',
    body: payload,
  });
}

export function unblockClient(barberId: number, clientId: number) {
  return request<void>(`/api/barbers/${barberId}/blocked-clients/${clientId}`, {
    method: 'DELETE',
  });
}

export function getOpenSlots(
  barberId: number,
  serviceId: number,
  from: string,
  to: string,
) {
  const query = `serviceId=${serviceId}&from=${from}&to=${to}`;
  return request<OpenSlots>(`/api/barbers/${barberId}/open-slots?${query}`);
}
