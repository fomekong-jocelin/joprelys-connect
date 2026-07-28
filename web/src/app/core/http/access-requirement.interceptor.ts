import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError } from 'rxjs';
import { AccessRequirementNoticeService } from './access-requirement-notice.service';

interface ApiErrorDetails {
  code?: unknown;
  message?: unknown;
  action?: unknown;
  required_scope?: unknown;
}

interface ApiErrorEnvelope {
  error?: ApiErrorDetails;
  detail?: unknown;
  title?: unknown;
}

export const accessRequirementInterceptor: HttpInterceptorFn = (req, next) => {
  const notices = inject(AccessRequirementNoticeService);

  return next(req).pipe(
    catchError((error: unknown) => {
      if (error instanceof HttpErrorResponse && error.status === 403) {
        const payload = error.error as ApiErrorEnvelope | null;
        const details = payload?.error;
        const rawCode = details?.code;
        const rawMessage = details?.message ?? payload?.detail ?? payload?.title;
        const code = typeof rawCode === 'string'
          ? rawCode
          : rawMessage === 'CONSENT_REQUIRED'
            ? 'CONSENT_REQUIRED'
            : null;

        if (code === 'CONSENT_REQUIRED' || code === 'SCOPE_REQUIRED') {
          const requiredScope = typeof details?.required_scope === 'string'
            ? details.required_scope
            : undefined;
          const fallback = code === 'CONSENT_REQUIRED'
            ? "Le patient n'a pas encore autorisé l'accès à cette partie de son dossier."
            : "L'accès existe, mais le périmètre demandé n'a pas été partagé.";

          notices.show({
            code,
            message: typeof rawMessage === 'string' && rawMessage !== code ? rawMessage : fallback,
            requiredScope,
          });
        }
      }

      return throwError(() => error);
    }),
  );
};
