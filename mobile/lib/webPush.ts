import { Platform } from 'react-native';

import * as pushApi from './api/push';

/**
 * Web Push for the Expo *web* target (native builds don't use this module).
 *
 * Push only reaches a phone when ALL of these hold, and each one fails silently in the browser,
 * so `getPushState()` names which one is missing and the UI tells the user what to do:
 *  - secure context (https — plain http://192.168… has no service workers at all)
 *  - iOS: the site must be installed on the Home Screen (Safari tabs have no PushManager)
 *  - the user granted permission — from a tap, browsers ignore cold prompts
 *  - a subscription registered on the backend for the logged-in user
 */
export type PushState =
  | 'native' // not the web target — nothing to do here
  | 'insecure'
  | 'needs-install' // iOS Safari tab: add to Home Screen first
  | 'unsupported'
  | 'denied'
  | 'prompt' // supported, permission not asked yet
  | 'ready'; // permission granted and a subscription exists

const WORKER_URL = '/service-worker.js';

function isIos(): boolean {
  const ua = navigator.userAgent;
  return /iPad|iPhone|iPod/.test(ua) || (ua.includes('Mac') && navigator.maxTouchPoints > 1);
}

function isStandalone(): boolean {
  return (
    window.matchMedia?.('(display-mode: standalone)').matches ||
    (navigator as unknown as { standalone?: boolean }).standalone === true
  );
}

function urlBase64ToUint8Array(base64Url: string): Uint8Array {
  const padding = '='.repeat((4 - (base64Url.length % 4)) % 4);
  const base64 = (base64Url + padding).replace(/-/g, '+').replace(/_/g, '/');
  const raw = atob(base64);
  return Uint8Array.from([...raw].map((c) => c.charCodeAt(0)));
}

function sameKey(a: ArrayBuffer | null | undefined, b: Uint8Array): boolean {
  if (!a) return false;
  const x = new Uint8Array(a);
  return x.length === b.length && x.every((v, i) => v === b[i]);
}

function support(): PushState | null {
  if (Platform.OS !== 'web' || typeof window === 'undefined') return 'native';
  if (!window.isSecureContext) return 'insecure';
  if (isIos() && !isStandalone()) return 'needs-install';
  if (!('serviceWorker' in navigator) || !('PushManager' in window) || !('Notification' in window)) {
    return 'unsupported';
  }
  return null;
}

async function currentSubscription(): Promise<PushSubscription | null> {
  const registration = await navigator.serviceWorker.getRegistration(WORKER_URL);
  return (await registration?.pushManager.getSubscription()) ?? null;
}

export async function getPushState(): Promise<PushState> {
  const blocked = support();
  if (blocked) return blocked;
  if (Notification.permission === 'denied') return 'denied';
  if (Notification.permission === 'default') return 'prompt';
  return (await currentSubscription()) ? 'ready' : 'prompt';
}

/** (Re)creates the browser subscription and registers it for the logged-in user. */
async function subscribeAndRegister(): Promise<void> {
  await navigator.serviceWorker.register(WORKER_URL);
  const registration = await navigator.serviceWorker.ready;
  const { publicKey } = await pushApi.getVapidPublicKey();
  if (!publicKey) throw new Error('Push não está configurado no servidor.');
  const serverKey = urlBase64ToUint8Array(publicKey);

  let subscription = await registration.pushManager.getSubscription();
  // A subscription made under an old VAPID key can never receive pushes again (and subscribing
  // over it throws) — drop it and start clean.
  if (subscription && !sameKey(subscription.options.applicationServerKey, serverKey)) {
    await subscription.unsubscribe();
    subscription = null;
  }
  if (!subscription) {
    subscription = await registration.pushManager.subscribe({
      userVisibleOnly: true,
      applicationServerKey: serverKey as BufferSource,
    });
  }

  const json = subscription.toJSON();
  if (!json.endpoint || !json.keys) throw new Error('Assinatura de push inválida.');
  // Upsert by endpoint: also re-points the device at whoever is logged in now.
  await pushApi.subscribePush({
    platform: 'WEB',
    endpoint: json.endpoint,
    p256dh: json.keys.p256dh,
    authKey: json.keys.auth,
  });
}

/** Call from a tap. Asks for permission if needed, then subscribes. */
export async function enablePush(): Promise<PushState> {
  const blocked = support();
  if (blocked) return blocked;
  if (Notification.permission === 'default') {
    await Notification.requestPermission();
  }
  if (Notification.permission !== 'granted') return getPushState();
  try {
    await subscribeAndRegister();
  } catch {
    return 'prompt';
  }
  return 'ready';
}

/**
 * Silent re-sync on every app start / login for users who already granted permission. Keeps the
 * backend row alive when the browser rotates the subscription or another account used this device.
 */
export async function syncPush(): Promise<PushState> {
  const blocked = support();
  if (blocked) return blocked;
  if (Notification.permission !== 'granted') return getPushState();
  try {
    await subscribeAndRegister();
    return 'ready';
  } catch {
    return 'prompt';
  }
}

/** Detach this device from the current user. Must run BEFORE the auth token is dropped. */
export async function disablePush(): Promise<void> {
  if (support()) return;
  try {
    const subscription = await currentSubscription();
    if (subscription) {
      await pushApi.unsubscribePush({ endpoint: subscription.endpoint });
    }
  } catch {
    // Best effort — the next login re-points the endpoint at the right user anyway.
  }
}

export async function sendTestPush(): Promise<number> {
  const { delivered } = await pushApi.sendTestPush();
  return delivered;
}
