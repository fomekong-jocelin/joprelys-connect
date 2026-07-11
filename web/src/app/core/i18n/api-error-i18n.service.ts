import { inject, Injectable } from '@angular/core';
import { I18nService } from './i18n.service';

interface ApiErrorPayload {
  code?: unknown;
  detail?: unknown;
  message?: unknown;
}

@Injectable({ providedIn: 'root' })
export class ApiErrorI18nService {
  private readonly i18n = inject(I18nService);

  message(error: unknown, translationPrefix: string, fallbackKey: string): string {
    const candidate = this.errorCode(error);
    if (candidate) {
      const translationKey = `${translationPrefix}.${candidate}`;
      const translated = this.i18n.t(translationKey);
      if (translated !== translationKey) {
        return translated;
      }
    }
    return this.i18n.t(fallbackKey);
  }

  private errorCode(error: unknown): string | null {
    const apiError = error as { error?: ApiErrorPayload };
    const candidate = apiError.error?.code ?? apiError.error?.detail ?? apiError.error?.message;
    return typeof candidate === 'string' && /^[A-Z0-9_]+$/.test(candidate)
      ? candidate
      : null;
  }
}
