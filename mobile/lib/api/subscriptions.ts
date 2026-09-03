import { request } from '../api';
import type { PixQrCodeResponse, SubscriptionStatusResponse } from '../types';

export function getSubscriptionStatus() {
  return request<SubscriptionStatusResponse>('/api/subscriptions/me');
}

export function requestSubscriptionPix() {
  return request<PixQrCodeResponse>('/api/subscriptions/me/pix', { method: 'POST' });
}
