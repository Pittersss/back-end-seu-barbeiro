export type UserRole = 'CLIENT' | 'BARBER' | 'ADMIN';

export type AppointmentStatus = 'PENDING' | 'CONFIRMED' | 'COMPLETED' | 'CANCELLED';

export type PaymentMethod = 'PIX' | 'CARD' | 'CASH';

export type RequestStatus = 'PENDING' | 'APPROVED' | 'REJECTED';

export interface AuthResponse {
  token: string;
  userId: number;
  role: UserRole;
  name: string;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterClientRequest {
  name: string;
  email: string;
  password: string;
  phone?: string;
}

export interface RegisterBarberRequest {
  name: string;
  email: string;
  password: string;
  phone?: string;
  pixKey?: string;
}

export interface VerifyEmailRequest {
  email: string;
  code: string;
}

export interface ResendCodeRequest {
  email: string;
}

export interface BarberShop {
  id: number;
  name: string;
  address?: string;
  phone?: string;
  photoBase64?: string | null;
  acceptingBarbers: boolean;
  ownerId?: number;
  ownerName?: string;
}

export interface BarberShopPostPut {
  name: string;
  address?: string;
  phone?: string;
  photoBase64?: string | null;
}

export interface BarberShopRequestPayload {
  shopName: string;
  shopAddress?: string;
  shopPhone?: string;
}

export interface BarberShopRequestResponse {
  id: number;
  status: RequestStatus;
  shopName: string;
  shopAddress?: string;
  shopPhone?: string;
  requesterId: number;
  requesterName: string;
  createdAt: string;
}

export interface Barber {
  id: number;
  name: string;
  email: string;
  phone?: string;
  avatarBase64?: string | null;
  pixKey?: string;
  available: boolean;
  delayTolerance: number;
  workStartHour: number;
  workEndHour: number;
  breakStartHour: number | null;
  breakEndHour: number | null;
  barberShopId?: number;
}

export interface BarberPostPut {
  name: string;
  phone?: string;
  pixKey?: string;
  delayTolerance: number;
  workStartHour: number;
  workEndHour: number;
  breakStartHour: number | null;
  breakEndHour: number | null;
}

export interface TimeBlock {
  id: number;
  startsAt: string;
  endsAt: string;
  reason?: string | null;
  barberId: number;
}

export interface TimeBlockPost {
  startsAt: string;
  endsAt: string;
  reason?: string | null;
}

export interface BlockedClient {
  id: number;
  clientId: number;
  clientName: string;
  clientPhone?: string | null;
  reason?: string | null;
  createdAt: string;
}

export interface BlockClientPost {
  clientId: number;
  reason?: string | null;
}

/** Map of `YYYY-MM-DD` → bookable local-ISO start times for that day. */
export type OpenSlots = Record<string, string[]>;

export interface Service {
  id: number;
  name: string;
  description?: string;
  durationMinutes: number;
  price: number;
  available: boolean;
  barberShopId: number;
  barberId?: number | null;
  barberName?: string | null;
}

export interface ServicePostPut {
  name: string;
  description?: string;
  durationMinutes: number;
  price: number;
}

export interface Product {
  id: number;
  name: string;
  description?: string;
  price: number;
  available: boolean;
  barberShopId: number;
}

export interface ProductPostPut {
  name: string;
  description?: string;
  price: number;
}

export interface Appointment {
  id: number;
  scheduledAt: string;
  status: AppointmentStatus;
  paymentMethod: PaymentMethod;
  clientId: number;
  clientName: string;
  clientAvatarBase64?: string | null;
  barberId: number;
  barberName: string;
  barberAvatarBase64?: string | null;
  serviceId: number;
  serviceName: string;
}

export interface AppointmentPost {
  barberId: number;
  serviceId: number;
  scheduledAt: string;
  paymentMethod: PaymentMethod;
}

export interface PixQrCodeResponse {
  appointmentId: number | null;
  pixKey: string;
  amount: number;
  merchantName: string;
  merchantCity: string;
  txId: string | null;
  pixCopyPaste: string;
  qrCodeBase64: string;
}

export type SubscriptionStatus = 'ACTIVE' | 'PENDING_CONFIRMATION' | 'INACTIVE';

export interface SubscriptionStatusResponse {
  status: SubscriptionStatus;
  periodEnd?: string | null;
}

export interface UserProfile {
  id: number;
  name: string;
  email: string;
  role: UserRole;
  phone?: string | null;
  avatarBase64?: string | null;
}

export interface UpdateProfilePayload {
  name: string;
  phone?: string | null;
  avatarBase64?: string | null;
}
