import { request } from '../api';
import type { Service } from '../types';

export function listServices(shopId: number) {
  return request<Service[]>(`/api/barbershops/${shopId}/services`);
}
