import { request } from '../api';
import type { UpdateProfilePayload, UserProfile } from '../types';

export function getMe() {
  return request<UserProfile>('/api/users/me');
}

export function updateMe(payload: UpdateProfilePayload) {
  return request<UserProfile>('/api/users/me', { method: 'PUT', body: payload });
}
