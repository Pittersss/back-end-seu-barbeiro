import { request } from '../api';
import type { PixQrCodeResponse } from '../types';

export function getPixQrCode(appointmentId: number) {
  return request<PixQrCodeResponse>(`/api/appointments/${appointmentId}/pix`);
}
