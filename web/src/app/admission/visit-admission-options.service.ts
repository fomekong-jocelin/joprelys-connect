import { Injectable, inject, signal } from '@angular/core';
import { VisitApiService } from '../visit/visit-api.service';
import { AdmissionPractitionerOption } from '../visit/visit.models';

/** Valeur technique du choix « Autre » dans la liste des services. */
export const OTHER_SERVICE = '__OTHER__';

/**
 * Référentiels communs aux formulaires d'admission, fournis par le backend sans droit
 * de gestion du personnel : services de l'établissement et cliniciens (médecins, infirmiers).
 */
@Injectable()
export class VisitAdmissionOptionsService {
  private readonly visitApi = inject(VisitApiService);

  readonly services = signal<string[]>([]);
  readonly loadError = signal(false);
  private readonly clinicians = signal<AdmissionPractitionerOption[]>([]);

  private loaded = false;

  /** Charge les référentiels une seule fois par formulaire d'admission. */
  load(): void {
    if (this.loaded) return;
    this.loaded = true;
    this.visitApi.getAdmissionOptions().subscribe({
      next: (options) => {
        this.services.set(options.services);
        this.clinicians.set(options.practitioners);
        this.loadError.set(false);
      },
      error: () => {
        this.loaded = false;
        this.loadError.set(true);
      },
    });
  }

  practitionerName(id: string | null | undefined): string | null {
    return id ? this.clinicians().find((member) => member.id === id)?.displayName ?? null : null;
  }

  /** Cliniciens affectés au service choisi ; tous les cliniciens si le service n'a pas d'affectés. */
  practitionersFor(service: string | null | undefined): AdmissionPractitionerOption[] {
    const clinicians = this.clinicians();
    const normalized = service?.trim().toLowerCase();
    if (!normalized || service === OTHER_SERVICE) return clinicians;
    const byService = clinicians.filter((member) =>
      member.unitNames.some((name) => name.trim().toLowerCase() === normalized),
    );
    return byService.length > 0 ? byService : clinicians;
  }
}
