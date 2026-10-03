import { Injectable, inject, signal } from '@angular/core';
import { catchError, of } from 'rxjs';
import { HospitalOrganizationApiService } from '../clinic/hospital-organization/hospital-organization-api.service';
import { StaffApiService } from '../clinic/staff/staff-api.service';
import { StaffMember } from '../clinic/staff/staff.models';

/** Valeur technique du choix « Autre » dans la liste des services. */
export const OTHER_SERVICE = '__OTHER__';

/**
 * Référentiels communs aux formulaires d'admission : services de l'établissement
 * (catalogue configuré) et praticiens pouvant être praticien principal d'une visite.
 */
@Injectable()
export class VisitAdmissionOptionsService {
  private readonly hospitalOrgApi = inject(HospitalOrganizationApiService);
  private readonly staffApi = inject(StaffApiService);

  readonly services = signal<string[]>([]);
  private readonly clinicians = signal<StaffMember[]>([]);

  load(): void {
    this.hospitalOrgApi.listServiceCatalog().pipe(catchError(() => of([]))).subscribe((entries) => {
      const names = entries.map((entry) => entry.nameFr || entry.nameEn || entry.code).filter(Boolean);
      this.services.set(Array.from(new Set(names)).sort((a, b) => a.localeCompare(b)));
    });
    this.staffApi.list().pipe(catchError(() => of([] as StaffMember[]))).subscribe((members) => {
      this.clinicians.set(members.filter((member) => member.enabled && this.isClinician(member)));
    });
  }

  /** Médecins et infirmiers du service choisi ; tous les cliniciens si le service n'a pas d'affectés. */
  practitionersFor(service: string | null | undefined): StaffMember[] {
    const clinicians = this.clinicians();
    const normalized = service?.trim().toLowerCase();
    if (!normalized || service === OTHER_SERVICE) return clinicians;
    const byService = clinicians.filter((member) =>
      (member.activeOrganizationalUnits ?? []).some((unit) =>
        [unit.nameFr, unit.nameEn, unit.code].some((name) => name?.trim().toLowerCase() === normalized),
      ) || member.department?.trim().toLowerCase() === normalized,
    );
    return byService.length > 0 ? byService : clinicians;
  }

  private isClinician(member: StaffMember): boolean {
    const roles = (member.role ?? '').split(',').map((role) => role.trim());
    return roles.includes('MEDECIN') || roles.includes('INFIRMIER');
  }
}
