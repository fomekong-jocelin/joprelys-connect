import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, catchError, of, tap } from 'rxjs';

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

const LOCAL_STORAGE_KEY = 'joprelys_demo_leads_backup';
const OFFICIAL_WHATSAPP_PHONE = '237691893198';

@Injectable({ providedIn: 'root' })
export class LandingDemoService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = '/api/public/demo-requests';

  submitDemo(payload: DemoLeadPayload): Observable<DemoLeadResponse | null> {
    // 1. Safety First: Persist lead locally immediately so no message is EVER lost
    this.saveLeadLocally(payload, 'PENDING');

    return this.http.post<DemoLeadResponse>(this.apiUrl, payload).pipe(
      tap((response) => {
        // Mark locally saved lead as synced with server ID
        this.updateLocalLeadStatus(payload.phone, 'SYNCED', response.id);
      }),
      catchError((error) => {
        // Network/offline error: the lead remains stored locally for subsequent sync
        console.warn('[LandingDemoService] Backend unreachable, lead preserved in offline storage.', error);
        return of(null);
      })
    );
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
    return `https://wa.me/${OFFICIAL_WHATSAPP_PHONE}?text=${encodeURIComponent(message)}`;
  }

  private saveLeadLocally(payload: DemoLeadPayload, syncStatus: 'PENDING' | 'SYNCED'): void {
    try {
      const existingRaw = localStorage.getItem(LOCAL_STORAGE_KEY);
      const leads = existingRaw ? JSON.parse(existingRaw) : [];
      leads.push({
        ...payload,
        syncStatus,
        timestamp: new Date().toISOString()
      });
      localStorage.setItem(LOCAL_STORAGE_KEY, JSON.stringify(leads));
    } catch {
      // LocalStorage might be disabled in private mode
    }
  }

  private updateLocalLeadStatus(phone: string, syncStatus: string, serverId?: string): void {
    try {
      const existingRaw = localStorage.getItem(LOCAL_STORAGE_KEY);
      if (!existingRaw) return;
      const leads = JSON.parse(existingRaw);
      const lead = leads.find((l: any) => l.phone === phone);
      if (lead) {
        lead.syncStatus = syncStatus;
        if (serverId) lead.serverId = serverId;
        localStorage.setItem(LOCAL_STORAGE_KEY, JSON.stringify(leads));
      }
    } catch {
      // ignore
    }
  }
}
