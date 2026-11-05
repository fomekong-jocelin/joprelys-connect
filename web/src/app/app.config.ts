import { DOCUMENT, registerLocaleData } from '@angular/common';
import localeFr from '@angular/common/locales/fr';
import { ApplicationConfig, LOCALE_ID, inject, provideAppInitializer, provideBrowserGlobalErrorListeners } from '@angular/core';
import { provideHttpClient, withFetch, withInterceptors } from '@angular/common/http';
import { provideRouter, withNavigationErrorHandler } from '@angular/router';

import { AuthApiService } from './auth/auth-api.service';
import { AuthSessionKeepAliveService } from './auth/auth-session-keep-alive.service';
import { authTokenInterceptor } from './auth/auth-token.interceptor';
import { accessRequirementInterceptor } from './core/http/access-requirement.interceptor';
import { I18nService } from './core/i18n/i18n.service';
import { reloadOnStaleChunk } from './core/routing/stale-chunk-recovery';
import { legalRoutes } from './legal/legal.routes';
import { routes } from './app.routes';

// Données de locale françaises (noms de mois, séparateurs) pour les pipes Angular.
registerLocaleData(localeFr);

/** Locale des pipes Angular, alignée sur la langue choisie au démarrage (le français par défaut). */
export function resolveAppLocaleId(language: string): 'fr' | 'en' {
  return language === 'en' ? 'en' : 'fr';
}

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    { provide: LOCALE_ID, useFactory: () => resolveAppLocaleId(inject(I18nService).currentLanguage()) },
    provideRouter(
      [...legalRoutes, ...routes],
      withNavigationErrorHandler((error) => {
        reloadOnStaleChunk(error, inject(DOCUMENT).defaultView);
      }),
    ),
    provideHttpClient(withFetch(), withInterceptors([authTokenInterceptor, accessRequirementInterceptor])),
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
