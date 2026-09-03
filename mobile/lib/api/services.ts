import { request } from '../api';
import type { Service, ServicePostPut } from '../types';

export function listServices(shopId: number) {
  return request<Service[]>(`/api/barbershops/${shopId}/services`);
}

export function createService(shopId: number, payload: ServicePostPut) {
  return request<Service>(`/api/barbershops/${shopId}/services`, {
    method: 'POST',
    body: payload,
  });
}

export function updateService(shopId: number, serviceId: number, payload: ServicePostPut) {
  return request<Service>(`/api/barbershops/${shopId}/services/${serviceId}`, {
    method: 'PUT',
    body: payload,
  });
}

export function deleteService(shopId: number, serviceId: number) {
  return request<void>(`/api/barbershops/${shopId}/services/${serviceId}`, {
    method: 'DELETE',
  });
}

export function toggleServiceAvailability(shopId: number, serviceId: number) {
  return request<Service>(`/api/barbershops/${shopId}/services/${serviceId}/availability`, {
    method: 'PATCH',
  });
}
