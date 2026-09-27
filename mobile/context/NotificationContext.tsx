import { createContext, useContext, useEffect, useMemo, useRef, useState, type ReactNode } from 'react';
import { router } from 'expo-router';
import { AppState, Platform } from 'react-native';

import { useAuth } from './AuthContext';
import * as notificationsApi from '../lib/api/notifications';
import * as webPush from '../lib/webPush';
import type { PushState } from '../lib/webPush';
import type { NotificationItem } from '../lib/types';

// Cheap unread-count poll; the full list is only refetched when the count changes.
const POLL_INTERVAL_MS = 3000;

interface NotificationContextValue {
  notifications: NotificationItem[];
  unreadCount: number;
  refresh: () => Promise<void>;
  markAllRead: () => Promise<void>;
  markRead: (id: number) => Promise<void>;
  /** Web push readiness on this device — drives the "ativar notificações" banner. */
  pushState: PushState;
  enablePush: () => Promise<void>;
  sendTestPush: () => Promise<number>;
}

const NotificationContext = createContext<NotificationContextValue | undefined>(undefined);

export function NotificationProvider({ children }: { children: ReactNode }) {
  const { session } = useAuth();
  const [notifications, setNotifications] = useState<NotificationItem[]>([]);
  const [unreadCount, setUnreadCount] = useState(0);
  const [pushState, setPushState] = useState<PushState>('native');

  const lastCount = useRef(-1);

  async function refresh(force = true) {
    if (!session) return;
    try {
      if (!force) {
        const { count } = await notificationsApi.getUnreadCount();
        if (count === lastCount.current) return;
      }
      const [list, { count }] = await Promise.all([
        notificationsApi.listNotifications(),
        notificationsApi.getUnreadCount(),
      ]);
      lastCount.current = count;
      setNotifications(list);
      setUnreadCount(count);
    } catch {
      // Transient failures are fine — the next poll retries.
    }
  }

  useEffect(() => {
    if (!session) {
      setNotifications([]);
      setUnreadCount(0);
      return;
    }
    lastCount.current = -1;
    refresh();
    // Silent for users who already granted permission; a first-time prompt needs a tap (banner).
    webPush.syncPush().then(setPushState);
    const interval = setInterval(() => refresh(false), POLL_INTERVAL_MS);
    const sub = AppState.addEventListener('change', (state) => {
      if (state === 'active') refresh();
    });
    // The service worker pings open tabs the moment a push lands, so the bell updates
    // instantly instead of waiting for the next poll.
    const onWorkerMessage = (event: MessageEvent) => {
      if (event.data?.type === 'navigate' && typeof event.data.url === 'string') {
        router.push(event.data.url as never);
      }
      refresh();
    };
    const canListen = Platform.OS === 'web' && typeof navigator !== 'undefined' && 'serviceWorker' in navigator;
    if (canListen) navigator.serviceWorker.addEventListener('message', onWorkerMessage);
    return () => {
      clearInterval(interval);
      sub.remove();
      if (canListen) navigator.serviceWorker.removeEventListener('message', onWorkerMessage);
    };
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
      pushState,
      enablePush: async () => {
        setPushState(await webPush.enablePush());
      },
      sendTestPush: webPush.sendTestPush,
      markRead: async (id: number) => {
        const target = notifications.find((n) => n.id === id);
        if (!target || target.read) return;
        await notificationsApi.markNotificationRead(id);
        setNotifications((prev) => prev.map((n) => (n.id === id ? { ...n, read: true } : n)));
        setUnreadCount((prev) => Math.max(0, prev - 1));
      },
    }),
    [notifications, unreadCount, pushState],
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
