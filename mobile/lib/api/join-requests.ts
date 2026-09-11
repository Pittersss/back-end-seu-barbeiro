import { request } from '../api';
import type { JoinRequestResponse } from '../types';

export function requestToJoinShop(shopId: number, message?: string) {
  return request<JoinRequestResponse>(`/api/barbershops/${shopId}/join-requests`, {
    method: 'POST',
    body: { message },
  });
}

export function listJoinRequests(shopId: number) {
  return request<JoinRequestResponse[]>(`/api/barbershops/${shopId}/join-requests`);
}

export function decideJoinRequest(shopId: number, requestId: number, accepted: boolean) {
  return request<JoinRequestResponse>(`/api/barbershops/${shopId}/join-requests/${requestId}`, {
    method: 'PATCH',
    body: { accepted },
  });
}
