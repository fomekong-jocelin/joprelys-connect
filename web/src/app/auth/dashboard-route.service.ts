import { Service, computed, inject } from '@angular/core';
import { AuthTokenStorageService, hasPatientRole } from './auth-token-storage.service';

export const PROFESSIONAL_DASHBOARD_URL = '/dashboard';
export const PATIENT_DASHBOARD_URL = '/patient/dashboard';
export const PUBLIC_HOME_URL = '/';

/** Point d'entrée « Accueil » de l'utilisateur courant : tableau de bord du rôle, ou accueil public. */
@Service()
export class DashboardRouteService {
  private readonly tokenStorage = inject(AuthTokenStorageService);

  readonly hasSession = computed(() => this.tokenStorage.session() !== null);

  readonly url = computed(() => {
    const session = this.tokenStorage.session();
    if (!session) return PUBLIC_HOME_URL;
    return hasPatientRole(session) ? PATIENT_DASHBOARD_URL : PROFESSIONAL_DASHBOARD_URL;
  });
}
