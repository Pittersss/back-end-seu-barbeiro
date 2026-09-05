import { request } from '../api';
import type { PushSubscriptionPayload, PushUnsubscribePayload } from '../types';

export function getVapidPublicKey() {
  return request<{ publicKey: string }>('/api/push/vapid-public-key');
}

export function subscribePush(payload: PushSubscriptionPayload) {
  return request<void>('/api/push/subscriptions', { method: 'POST', body: payload });
}

export function unsubscribePush(payload: PushUnsubscribePayload) {
  return request<void>('/api/push/subscriptions', { method: 'DELETE', body: payload });
}
