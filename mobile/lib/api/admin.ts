import { request } from '../api';
import type {
  Appointment,
  BarberShopRequestResponse,
  Page,
  SubscriptionPaymentResponse,
  UserProfile,
} from '../types';

export function listBarberShopRequests() {
  return request<BarberShopRequestResponse[]>('/api/admin/barbershop-requests');
}

export function decideBarberShopRequest(id: number, approved: boolean) {
  return request<BarberShopRequestResponse>(`/api/admin/barbershop-requests/${id}`, {
    method: 'PATCH',
    body: { approved },
  });
}

export function listPendingSubscriptionPayments() {
  return request<SubscriptionPaymentResponse[]>('/api/admin/subscription-payments');
}

export function decideSubscriptionPayment(id: number, approved: boolean) {
  return request<SubscriptionPaymentResponse>(`/api/admin/subscription-payments/${id}`, {
    method: 'PATCH',
    body: { approved },
  });
}

export function listClients() {
  return request<UserProfile[]>('/api/admin/clients');
}

export function deleteClient(id: number) {
  return request<void>(`/api/admin/clients/${id}`, { method: 'DELETE' });
}

export function deleteBarberShop(id: number) {
  return request<void>(`/api/admin/barbershops/${id}`, { method: 'DELETE' });
}

export function listAdminAppointments(page: number, size = 20) {
  return request<Page<Appointment>>(`/api/admin/appointments?page=${page}&size=${size}`);
}
