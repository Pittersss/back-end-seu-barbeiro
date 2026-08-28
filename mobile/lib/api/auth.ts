import { request } from '../api';
import type { AuthResponse, LoginRequest, RegisterBarberRequest, RegisterClientRequest } from '../types';

export function login(payload: LoginRequest) {
  return request<AuthResponse>('/api/auth/login', { method: 'POST', body: payload });
}

export function registerClient(payload: RegisterClientRequest) {
  return request<AuthResponse>('/api/auth/register/client', { method: 'POST', body: payload });
}

export function registerBarber(payload: RegisterBarberRequest) {
  return request<AuthResponse>('/api/auth/register/barber', { method: 'POST', body: payload });
}
