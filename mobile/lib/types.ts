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

export interface BarberShop {
  id: number;
  name: string;
  address?: string;
  phone?: string;
  acceptingBarbers: boolean;
  ownerId?: number;
  ownerName?: string;
}

export interface BarberShopPostPut {
  name: string;
  address?: string;
  phone?: string;
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
  pixKey?: string;
  available: boolean;
  delayTolerance: number;
  barberShopId?: number;
}

export interface BarberPostPut {
  name: string;
  phone?: string;
  pixKey?: string;
  delayTolerance: number;
}

export interface Service {
  id: number;
  name: string;
  description?: string;
  durationMinutes: number;
  price: number;
  available: boolean;
  barberShopId: number;
}

export interface Appointment {
  id: number;
  scheduledAt: string;
  status: AppointmentStatus;
  paymentMethod: PaymentMethod;
  clientId: number;
  clientName: string;
  barberId: number;
  barberName: string;
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
  appointmentId: number;
  pixKey: string;
  amount: number;
  merchantName: string;
  merchantCity: string;
  txId: string;
  pixCopyPaste: string;
  qrCodeBase64: string;
}
