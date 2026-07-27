import { ApplicationConfig, inject, provideAppInitializer, provideBrowserGlobalErrorListeners } from '@angular/core';
import { provideHttpClient, withFetch, withInterceptors } from '@angular/common/http';
import { provideRouter } from '@angular/router';

import { AuthApiService } from './auth/auth-api.service';
import { AuthSessionKeepAliveService } from './auth/auth-session-keep-alive.service';
import { authTokenInterceptor } from './auth/auth-token.interceptor';
import { I18nService } from './core/i18n/i18n.service';
import { legalRoutes } from './legal/legal.routes';
import { routes } from './app.routes';
import { ClinicalRealtimeVoiceBridgeService } from './consultation/clinical-realtime-voice-bridge.service';
import { RealtimeVoiceBridgeService } from './consultation/realtime-voice-bridge.service';

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideRouter([...legalRoutes, ...routes]),
    provideHttpClient(withFetch(), withInterceptors([authTokenInterceptor])),
    {
      provide: RealtimeVoiceBridgeService,
      useClass: ClinicalRealtimeVoiceBridgeService,
    },
    provideAppInitializer(() => {
      const i18n = inject(I18nService);
      return i18n.init();
    }),
    provideAppInitializer(() => {
      const authApi = inject(AuthApiService);
      return authApi.restoreSession();
    }),
    provideAppInitializer(() => {
      const keepAlive = inject(AuthSessionKeepAliveService);
      keepAlive.start();
    }),
  ]
};
