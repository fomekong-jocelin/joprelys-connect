import { ApplicationConfig, inject, provideAppInitializer, provideBrowserGlobalErrorListeners } from '@angular/core';
import { provideHttpClient, withFetch, withInterceptors } from '@angular/common/http';
import { provideRouter } from '@angular/router';

import { AuthApiService } from './auth/auth-api.service';
import { AuthSessionKeepAliveService } from './auth/auth-session-keep-alive.service';
import { authTokenInterceptor } from './auth/auth-token.interceptor';
import { accessRequirementInterceptor } from './core/http/access-requirement.interceptor';
import { I18nService } from './core/i18n/i18n.service';
import { legalRoutes } from './legal/legal.routes';
import { routes } from './app.routes';

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideRouter([...legalRoutes, ...routes]),
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
