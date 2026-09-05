import { request } from '../api';
import type { NotificationItem } from '../types';

export function listNotifications() {
  return request<NotificationItem[]>('/api/notifications');
}

export function getUnreadCount() {
  return request<{ count: number }>('/api/notifications/unread-count');
}

export function markNotificationRead(id: number) {
  return request<void>(`/api/notifications/${id}/read`, { method: 'PATCH' });
}

export function markAllNotificationsRead() {
  return request<void>('/api/notifications/read-all', { method: 'PATCH' });
}
