import { HttpErrorResponse } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { I18nService } from '../core/i18n/i18n.service';

@Injectable({ providedIn: 'root' })
export class RealtimeVoiceConnectionSupportService {
  private readonly i18n = inject(I18nService);

  waitForIceGathering(pc: RTCPeerConnection): Promise<void> {
    if (pc.iceGatheringState === 'complete') return Promise.resolve();
    return new Promise(resolve => {
      const listener = () => {
        if (pc.iceGatheringState !== 'complete') return;
        pc.removeEventListener('icegatheringstatechange', listener);
        resolve();
      };
      pc.addEventListener('icegatheringstatechange', listener);
      setTimeout(() => {
        pc.removeEventListener('icegatheringstatechange', listener);
        resolve();
      }, 5000);
    });
  }

  waitForDataChannel(channel: RTCDataChannel, pc: RTCPeerConnection): Promise<void> {
    if (channel.readyState === 'open') return Promise.resolve();
    return new Promise((resolve, reject) => {
      const timeout = setTimeout(() => {
        cleanup();
        reject(new Error('AI_REALTIME_CHANNEL_TIMEOUT'));
      }, 15000);
      const open = () => {
        cleanup();
        resolve();
      };
      const failed = () => {
        if (pc.connectionState !== 'failed') return;
        cleanup();
        reject(new Error('AI_REALTIME_PEER_CONNECTION_FAILED'));
      };
      const cleanup = () => {
        clearTimeout(timeout);
        channel.removeEventListener('open', open);
        pc.removeEventListener('connectionstatechange', failed);
      };
      channel.addEventListener('open', open);
      pc.addEventListener('connectionstatechange', failed);
    });
  }

  describeConnectionError(error: unknown): string {
    if (error instanceof HttpErrorResponse) {
      const reason = this.extractBackendReason(error);
      if (reason === 'AI_REALTIME_NOT_CONFIGURED') {
        return this.i18n.t(
          'consultation.ai.realtimeNotConfigured',
          'Le temps réel n’est pas configuré sur le serveur.',
        );
      }
      if (reason === 'AI_REALTIME_AUTHENTICATION_FAILED' || reason === 'AI_REALTIME_ACCESS_DENIED') {
        return this.i18n.t(
          'consultation.ai.realtimeAccessDenied',
          'La clé OpenAI utilisée par le serveur n’autorise pas le mode Realtime.',
        );
      }
      if (reason === 'AI_REALTIME_QUOTA_EXCEEDED') {
        return this.i18n.t(
          'consultation.ai.realtimeQuotaExceeded',
          'Le quota OpenAI Realtime est épuisé ou temporairement limité.',
        );
      }
      if (reason === 'AI_SESSION_EXPIRED') {
        return this.i18n.t(
          'consultation.ai.realtimeSessionExpired',
          'La session IA a expiré. Relancez le copilote vocal.',
        );
      }
      if (reason === 'AI_REALTIME_MODEL_OR_CONFIG_UNAVAILABLE') {
        return this.i18n.t(
          'consultation.ai.realtimeModelUnavailable',
          'Le modèle Realtime ou sa configuration n’est pas disponible pour ce compte.',
        );
      }
      if (error.status === 0) {
        return this.i18n.t(
          'consultation.ai.realtimeNetworkError',
          'La connexion réseau au temps réel est impossible. Reconnexion automatique en cours.',
        );
      }
    }
    if (error instanceof Error && error.message === 'AI_REALTIME_SHARED_MIC_UNAVAILABLE') {
      return this.i18n.t(
        'consultation.ai.realtimeSharedMicUnavailable',
        'La capture de sécurité n’a pas de flux micro actif. Le temps réel reste désactivé pour éviter une capture non protégée.',
      );
    }
    if (error instanceof Error && error.message.startsWith('AI_REALTIME_')) {
      return this.i18n.t(
        'consultation.ai.realtimeConnectionFailed',
        'La liaison WebRTC n’a pas pu être établie. Reconnexion automatique en cours.',
      );
    }
    return this.i18n.t(
      'consultation.ai.realtimeUnavailable',
      'Le temps réel est momentanément indisponible. Reconnexion automatique en cours.',
    );
  }

  private extractBackendReason(error: HttpErrorResponse): string {
    if (typeof error.error === 'string') return error.error;
    if (error.error && typeof error.error === 'object') {
      const body = error.error as Record<string, unknown>;
      const value = body['detail'] ?? body['title'] ?? body['message'];
      return typeof value === 'string' ? value : '';
    }
    return '';
  }
}
