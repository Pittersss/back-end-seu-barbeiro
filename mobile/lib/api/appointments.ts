import { request } from '../api';
import type { Appointment, AppointmentPost, AppointmentStatus } from '../types';

export function listAppointments() {
  return request<Appointment[]>('/api/appointments');
}

export function createAppointment(payload: AppointmentPost) {
  return request<Appointment>('/api/appointments', { method: 'POST', body: payload });
}

export function updateAppointmentStatus(id: number, status: AppointmentStatus) {
  return request<Appointment>(`/api/appointments/${id}/status`, {
    method: 'PATCH',
    body: { status },
  });
}

export function cancelAppointment(id: number) {
  return request<void>(`/api/appointments/${id}/cancel`, { method: 'PATCH' });
}
