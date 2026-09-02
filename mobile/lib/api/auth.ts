import { request } from '../api';
import type {
  AuthResponse,
  LoginRequest,
  RegisterBarberRequest,
  RegisterClientRequest,
  ResendCodeRequest,
  VerifyEmailRequest,
} from '../types';

export function login(payload: LoginRequest) {
  return request<AuthResponse>('/api/auth/login', { method: 'POST', body: payload });
}

export function registerClient(payload: RegisterClientRequest) {
  return request<AuthResponse>('/api/auth/register/client', { method: 'POST', body: payload });
}

export function registerBarber(payload: RegisterBarberRequest) {
  return request<AuthResponse>('/api/auth/register/barber', { method: 'POST', body: payload });
}

export function verifyEmail(payload: VerifyEmailRequest) {
  return request<AuthResponse>('/api/auth/verify-email', { method: 'POST', body: payload });
}

export function resendCode(payload: ResendCodeRequest) {
  return request<void>('/api/auth/resend-code', { method: 'POST', body: payload });
}
