import { createContext, useContext, useEffect, useMemo, useRef, useState, type ReactNode } from 'react';
import { Platform } from 'react-native';

import { useAuth } from './AuthContext';
import * as notificationsApi from '../lib/api/notifications';
import * as pushApi from '../lib/api/push';
import type { NotificationItem } from '../lib/types';

const POLL_INTERVAL_MS = 30000;

interface NotificationContextValue {
  notifications: NotificationItem[];
  unreadCount: number;
  refresh: () => Promise<void>;
  markAllRead: () => Promise<void>;
}

const NotificationContext = createContext<NotificationContextValue | undefined>(undefined);

function urlBase64ToUint8Array(base64Url: string): Uint8Array {
  const padding = '='.repeat((4 - (base64Url.length % 4)) % 4);
  const base64 = (base64Url + padding).replace(/-/g, '+').replace(/_/g, '/');
  const raw = atob(base64);
  return Uint8Array.from([...raw].map((c) => c.charCodeAt(0)));
}

async function registerWebPush() {
  if (
    Platform.OS !== 'web' ||
    typeof navigator === 'undefined' ||
    !('serviceWorker' in navigator) ||
    typeof window === 'undefined' ||
    !('PushManager' in window) ||
    !('Notification' in window)
  ) {
    return;
  }
  try {
    if (Notification.permission === 'default') {
      const permission = await Notification.requestPermission();
      if (permission !== 'granted') return;
    }
    if (Notification.permission !== 'granted') return;

    const registration = await navigator.serviceWorker.register('/service-worker.js');
    const { publicKey } = await pushApi.getVapidPublicKey();
    if (!publicKey) return;

    let subscription = await registration.pushManager.getSubscription();
    if (!subscription) {
      subscription = await registration.pushManager.subscribe({
        userVisibleOnly: true,
        applicationServerKey: urlBase64ToUint8Array(publicKey) as BufferSource,
      });
    }

    const json = subscription.toJSON();
    if (!json.endpoint || !json.keys) return;
    await pushApi.subscribePush({
      platform: 'WEB',
      endpoint: json.endpoint,
      p256dh: json.keys.p256dh,
      authKey: json.keys.auth,
    });
  } catch {
    // Push is a progressive enhancement — never block the app on it.
  }
}

async function unregisterWebPush() {
  if (Platform.OS !== 'web' || typeof navigator === 'undefined' || !('serviceWorker' in navigator)) {
    return;
  }
  try {
    const registration = await navigator.serviceWorker.getRegistration();
    const subscription = await registration?.pushManager.getSubscription();
    if (subscription) {
      await pushApi.unsubscribePush({ endpoint: subscription.endpoint });
    }
  } catch {
    // Best-effort cleanup only.
  }
}

export function NotificationProvider({ children }: { children: ReactNode }) {
  const { session } = useAuth();
  const [notifications, setNotifications] = useState<NotificationItem[]>([]);
  const [unreadCount, setUnreadCount] = useState(0);
  const wasLoggedIn = useRef(false);

  async function refresh() {
    if (!session) return;
    try {
      const [list, { count }] = await Promise.all([
        notificationsApi.listNotifications(),
        notificationsApi.getUnreadCount(),
      ]);
      setNotifications(list);
      setUnreadCount(count);
    } catch {
      // Transient failures are fine — the next poll retries.
    }
  }

  useEffect(() => {
    if (!session) {
      if (wasLoggedIn.current) {
        unregisterWebPush();
      }
      wasLoggedIn.current = false;
      setNotifications([]);
      setUnreadCount(0);
      return;
    }
    wasLoggedIn.current = true;
    refresh();
    registerWebPush();
    const interval = setInterval(refresh, POLL_INTERVAL_MS);
    return () => clearInterval(interval);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [session?.userId]);

  const value = useMemo<NotificationContextValue>(
    () => ({
      notifications,
      unreadCount,
      refresh,
      markAllRead: async () => {
        if (unreadCount === 0) return;
        await notificationsApi.markAllNotificationsRead();
        setNotifications((prev) => prev.map((n) => ({ ...n, read: true })));
        setUnreadCount(0);
      },
    }),
    [notifications, unreadCount],
  );

  return <NotificationContext.Provider value={value}>{children}</NotificationContext.Provider>;
}

export function useNotifications() {
  const ctx = useContext(NotificationContext);
  if (!ctx) {
    throw new Error('useNotifications must be used within a NotificationProvider');
  }
  return ctx;
}
