import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, timeout } from 'rxjs';
import { APP_BRAND_CONFIG } from '../core/config/app-brand.config';

export interface DemoLeadPayload {
  fullName: string;
  organizationName: string;
  role?: string;
  phone: string;
  email?: string;
  city?: string;
  message?: string;
  source?: string;
  locale?: string;
}

export interface DemoLeadResponse {
  id: string;
  fullName: string;
  organizationName: string;
  status: string;
  createdAt: string;
  message: string;
}

@Injectable({ providedIn: 'root' })
export class LandingDemoService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = '/api/public/demo-requests';

  submitDemo(payload: DemoLeadPayload): Observable<DemoLeadResponse> {
    return this.http.post<DemoLeadResponse>(this.apiUrl, payload).pipe(timeout(20_000));
  }

  buildWhatsAppUrl(payload: DemoLeadPayload, lang: 'fr' | 'en' = 'fr'): string {
    const textFr = `Bonjour l'équipe Joprelys Connect,\n\nJe souhaite planifier une démonstration pour mon établissement.\n\n` +
      `👤 Nom : ${payload.fullName}\n` +
      `🏥 Établissement : ${payload.organizationName}\n` +
      `💼 Fonction : ${payload.role || 'Non précisée'}\n` +
      `📍 Ville : ${payload.city || 'Douala'}\n` +
      `📞 Téléphone : ${payload.phone}\n` +
      (payload.email ? `✉️ Email : ${payload.email}\n` : '') +
      (payload.message ? `🎯 Priorités : ${payload.message}\n` : '');

    const textEn = `Hello Joprelys Connect team,\n\nI would like to schedule a demo for my healthcare facility.\n\n` +
      `👤 Name: ${payload.fullName}\n` +
      `🏥 Facility: ${payload.organizationName}\n` +
      `💼 Role: ${payload.role || 'Not specified'}\n` +
      `📍 City: ${payload.city || 'Douala'}\n` +
      `📞 Phone: ${payload.phone}\n` +
      (payload.email ? `✉️ Email: ${payload.email}\n` : '') +
      (payload.message ? `🎯 Priorities: ${payload.message}\n` : '');

    const message = lang === 'en' ? textEn : textFr;
    return `https://wa.me/${APP_BRAND_CONFIG.contactWhatsAppPhone}?text=${encodeURIComponent(message)}`;
  }
}
