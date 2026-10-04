import { HospitalPractitioner, AdmissionVisit } from './hospitalization-workflow.models';
import { HospitalizationConsentPanelComponent } from './hospitalization-consent-panel.component';
import { HospitalizationOperatingReportPanelComponent } from './hospitalization-operating-report-panel.component';
import { CommonModule, DatePipe } from '@angular/common';
import { Component, Input, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { forkJoin, finalize } from 'rxjs';
import { HospitalServiceCatalogEntry, OrganizationalUnit } from '../clinic/hospital-organization/hospital-organization.models';
import { RbacApiService } from '../clinic/rbac/rbac-api.service';
import { BedConfiguration, FacilitySpace, UnitSpaceAssignment } from '../clinic/spatial/spatial-configuration.models';
import { I18nService } from '../core/i18n/i18n.service';
import { HospitalizationConsumptionPanelComponent } from './hospitalization-consumption-panel.component';
import { HospitalizationDailyCarePanelComponent } from './hospitalization-daily-care-panel.component';
import { HospitalizationLocationApiService } from './hospitalization-location-api.service';
import { StructuredHospitalization } from './hospitalization-location.models';
import { HospitalizationMedicationPanelComponent } from './hospitalization-medication-panel.component';
import { HospitalizationNotesPanelComponent } from './hospitalization-notes-panel.component';
import { HospitalizationStayHeaderComponent } from './hospitalization-stay-header.component';
import { PatientApiService } from './patient-api.service';
import { SpatialApiService } from './spatial-api.service';
import { isAdmissibleBed } from './bed-placement-policies';
import { isEligibleHospitalizationPractitioner } from './staff-placement-policies';

interface PlacementBed extends BedConfiguration {
  readonly roomNumber: string;
}

@Component({
  selector: 'app-patient-hospitalization',
  standalone: true,
  imports: [
    DatePipe,
    FormsModule,
    CommonModule,
    HospitalizationStayHeaderComponent,
    HospitalizationConsentPanelComponent,
    HospitalizationOperatingReportPanelComponent,
    HospitalizationNotesPanelComponent,
    HospitalizationDailyCarePanelComponent,
    HospitalizationMedicationPanelComponent,
    HospitalizationConsumptionPanelComponent,
  ],
  templateUrl: './patient-hospitalization.component.html',
})
export class PatientHospitalizationComponent implements OnInit {
  @Input({ required: true }) patientId!: string;

  private readonly patientApi = inject(PatientApiService);
  private readonly hospitalizationApi = inject(HospitalizationLocationApiService);
  private readonly spatialApi = inject(SpatialApiService);
  private readonly rbacApi = inject(RbacApiService);
  private readonly i18n = inject(I18nService);
  private bedRequest = 0;

  readonly t = (key: string, defaultValue?: string) => this.i18n.t(key, defaultValue);

  readonly list = signal<StructuredHospitalization[]>([]);
  readonly staffList = signal<HospitalPractitioner[]>([]);
  readonly patientVisits = signal<AdmissionVisit[]>([]);
  readonly visitsLoading = signal(false);
  readonly visitsError = signal<string | null>(null);

  readonly showAdmitModal = signal(false);
  readonly showDischargeModal = signal(false);
  readonly showPhysicalDepartureModal = signal(false);
  readonly showTransferModal = signal(false);
  readonly loading = signal(false);
  readonly loadError = signal<string | null>(null);
  readonly actionError = signal<string | null>(null);
  readonly admissionSubmitting = signal(false);
  readonly transferSubmitting = signal(false);
  readonly placementLoading = signal(false);
  readonly bedsLoading = signal(false);
  readonly admitError = signal<string | null>(null);
  readonly transferError = signal<string | null>(null);
  readonly transferSuccessMsg = signal<string | null>(null);
  readonly dischargeError = signal<string | null>(null);
  readonly physicalDepartureError = signal<string | null>(null);
  readonly physicalDepartureSubmitting = signal(false);

  activeTab = 'notes';

  readonly units = signal<OrganizationalUnit[]>([]);
  readonly serviceCatalog = signal<HospitalServiceCatalogEntry[]>([]);
  readonly spaces = signal<FacilitySpace[]>([]);
  readonly assignments = signal<UnitSpaceAssignment[]>([]);
  readonly freeBeds = signal<PlacementBed[]>([]);
  readonly selectedUnitId = signal('');
  readonly selectedSpaceId = signal('');
  readonly selectedBedId = signal('');

  // Adaptateurs de template transitoires : les IDs sont désormais ceux des unités HOS-ORG et des Space.
  readonly selectedWardId = this.selectedUnitId;
  readonly wards = computed(() => this.eligibleUnits().map((unit) => ({ id: unit.id, name: this.unitLabel(unit) })));

  admissionReason = '';
  visitId = '';
  responsiblePractitionerId = '';

  dischargeDiagnosis = '';
  dischargeInstructions = '';
  againstMedicalAdvice = false;
  physicalDepartureNote = '';

  readonly activeHospitalization = computed(() =>
    this.list().find((hospitalization) => hospitalization.status === 'EN_COURS') ?? null,
  );

  readonly pastHospitalizations = computed(() =>
    this.list().filter((hospitalization) => hospitalization.status !== 'EN_COURS'),
  );

  readonly eligibleUnits = computed(() => this.units().filter((unit) =>
    unit.active && (unit.unitType === 'SERVICE' || unit.unitType === 'CARE_UNIT')));

  readonly eligibleSpaces = computed(() => {
    const unitId = this.selectedUnitId();
    if (!unitId) return [];
    const allowedSpaceIds = new Set(this.assignments()
      .filter((assignment) => assignment.organizationalUnitId === unitId)
      .map((assignment) => assignment.spaceId));
    return this.spaces().filter((space) => space.active && space.inpatientProfile && allowedSpaceIds.has(space.id));
  });

  readonly staffMap = computed(() => {
    const map = new Map<string, string>();
    for (const practitioner of this.staffList()) map.set(practitioner.id, practitioner.displayName);
    return map;
  });

  readonly eligiblePractitioners = computed(() => this.staffList().filter((member) =>
    isEligibleHospitalizationPractitioner(member, this.selectedUnitId())));

  ngOnInit(): void {
    if (this.patientId) {
      this.loadHospitalizations();
      this.loadStaff();
      if (this.canAdmit()) this.loadVisits();
    }
  }

  loadStaff(): void {
    this.hospitalizationApi.practitioners().subscribe({ next: (data) => this.staffList.set(data), error: () => this.actionError.set(this.t('patients.hospitalization.workflow.loadStaffError')) });
  }

  loadVisits(): void {
    this.visitsLoading.set(true); this.visitsError.set(null);
    this.hospitalizationApi.admissionVisits(this.patientId).pipe(finalize(() => this.visitsLoading.set(false))).subscribe({
      next: (data) => this.patientVisits.set(data),
      error: () => { this.patientVisits.set([]); this.visitsError.set(this.t('patients.hospitalization.workflow.loadVisitsError')); },
    });
  }

  loadHospitalizations(): void {
    this.loading.set(true); this.loadError.set(null);
    this.hospitalizationApi.listForPatient(this.patientId).pipe(finalize(() => this.loading.set(false))).subscribe({
      next: (data) => {
        this.list.set(data);
      },
      error: () => this.loadError.set(this.t('patients.hospitalization.workflow.loadError')),
    });
  }

  hasRole(roleStr: string | undefined, allowedRoles: string[] | string): boolean {
    if (!roleStr) return false;
    const roles = roleStr.split(',').map((role) => role.trim());
    return Array.isArray(allowedRoles)
      ? roles.some((role) => allowedRoles.includes(role))
      : roles.includes(allowedRoles);
  }

  canAdmit(): boolean { return this.rbacApi.hasPermission('HOSPITALIZATION_ADMIT'); }
  canWriteNotes(): boolean { return this.rbacApi.hasPermission('HOSPITALIZATION_NOTE_WRITE'); }
  canRecordConsent(): boolean { return this.rbacApi.hasPermission('HOSPITALIZATION_CONSENT_RECORD'); }
  canWriteCare(): boolean { return this.rbacApi.hasPermission('HOSPITALIZATION_CARE_WRITE'); }
  canAdministerMedication(): boolean { return this.rbacApi.hasPermission('HOSPITALIZATION_MEDICATION_ADMINISTER'); }
  canRecordConsumable(): boolean { return this.rbacApi.hasPermission('HOSPITALIZATION_CONSUMABLE_RECORD'); }
  canWriteOperatingReport(): boolean { return this.rbacApi.hasPermission('CLINICAL_WRITE'); }

  canModify(): boolean {
    if (!this.activeHospitalization()) return this.canAdmit();
    switch (this.activeTab) {
      case 'notes': return this.canWriteNotes();
      case 'consents': return this.canRecordConsent();
      case 'cares': return this.canWriteCare();
      case 'meds': return this.canAdministerMedication();
      case 'consumptions': return this.canRecordConsumable();
      case 'cro': return this.canWriteOperatingReport();
      default: return false;
    }
  }

  openAdmitModal(): void {
    if (!this.canAdmit() || this.loading() || this.loadError()) return;
    this.admissionReason = '';
    this.visitId = '';
    this.responsiblePractitionerId = '';
    this.selectedUnitId.set('');
    this.selectedSpaceId.set('');
    this.selectedBedId.set('');
    this.freeBeds.set([]);
    this.admitError.set(null);
    this.loadVisits();
    this.loadPlacementOptions();
    this.showAdmitModal.set(true);
  }

  openTransferModal(): void {
    const active = this.activeHospitalization();
    if (!active || active.dischargeDecidedAt) return;
    this.transferError.set(null);
    this.transferSuccessMsg.set(null);
    this.selectedUnitId.set(active.currentServiceUnitId);
    this.selectedSpaceId.set(active.currentSpaceId);
    this.selectedBedId.set('');
    this.freeBeds.set([]);
    this.loadPlacementOptions(() => this.loadFreeBedsForUnit(active.currentServiceUnitId));
    this.showTransferModal.set(true);
  }

  onAdmissionWardChange(): void {
    this.selectedSpaceId.set('');
    this.selectedBedId.set('');
    this.freeBeds.set([]);
    if (!this.eligiblePractitioners().some((practitioner) => practitioner.id === this.responsiblePractitionerId)) {
      this.responsiblePractitionerId = '';
    }
    this.admitError.set(null); this.transferError.set(null);
    this.loadFreeBedsForUnit(this.selectedUnitId());
  }

  onAdmissionBedChange(): void {
    const bed = this.freeBeds().find((candidate) => candidate.id === this.selectedBedId());
    this.selectedSpaceId.set(bed?.spaceId ?? '');
  }

  saveAdmission(event: Event): void {
    event.preventDefault();
    this.onAdmissionBedChange();
    if (this.admissionSubmitting() || this.loading() || this.loadError() || this.visitsLoading() || this.visitsError() || this.placementLoading() || this.bedsLoading() || !this.canAdmit()
      || !this.selectedUnitId()
      || !this.selectedSpaceId()
      || !this.selectedBedId()
      || !this.admissionReason.trim()
      || !this.visitId
      || !this.responsiblePractitionerId) return;

    this.admitError.set(null);
    this.admissionSubmitting.set(true);
    this.hospitalizationApi.admit({
      patientId: this.patientId,
      serviceUnitId: this.selectedUnitId(),
      spaceId: this.selectedSpaceId(),
      bedId: this.selectedBedId(),
      admissionReason: this.admissionReason.trim(),
      visitId: this.visitId,
      responsiblePractitionerId: this.responsiblePractitionerId,
    }).pipe(finalize(() => this.admissionSubmitting.set(false))).subscribe({
      next: () => {
        this.showAdmitModal.set(false);
        this.loadHospitalizations();
      },
      error: (error) => {
        this.admitError.set(error.error?.detail || error.error?.title || this.t('patients.hospitalization.bedOccupied'));
        if (error.status === 409) this.loadFreeBedsForUnit(this.selectedUnitId());
      },
    });
  }

  saveTransfer(event: Event): void {
    event.preventDefault();
    const active = this.activeHospitalization();
    const bed = this.freeBeds().find((candidate) => candidate.id === this.selectedBedId());
    if (bed) this.selectedSpaceId.set(bed.spaceId);
    if (this.transferSubmitting() || this.placementLoading() || this.bedsLoading() || !active
      || active.dischargeDecidedAt
      || !this.selectedUnitId()
      || !this.selectedSpaceId()
      || !this.selectedBedId()) return;

    this.transferError.set(null);
    this.transferSuccessMsg.set(null);
    this.transferSubmitting.set(true);
    this.spatialApi.transferPatient({
      hospitalizationId: active.id,
      targetServiceUnitId: this.selectedUnitId(),
      targetSpaceId: this.selectedSpaceId(),
      targetBedId: this.selectedBedId(),
    }).pipe(finalize(() => this.transferSubmitting.set(false))).subscribe({
      next: () => {
        this.transferSuccessMsg.set(this.t('patients.hospitalization.transferSuccess'));
        setTimeout(() => {
          this.showTransferModal.set(false);
          this.loadHospitalizations();
        }, 800);
      },
      error: (error) => this.transferError.set(
        error.error?.detail || error.error?.title || this.t('patients.hospitalization.transferError'),
      ),
    });
  }

  retryPlacement(): void {
    this.admitError.set(null); this.transferError.set(null);
    if (this.showAdmitModal()) this.loadVisits();
    this.loadPlacementOptions(() => this.loadFreeBedsForUnit(this.selectedUnitId()));
  }

  unitLabel(unit: OrganizationalUnit): string {
    if (unit.name) return unit.name;
    const catalog = this.serviceCatalog().find((entry) => entry.code === unit.serviceCatalogCode);
    if (!catalog) return unit.code;
    return this.i18n.currentLanguage() === 'en' ? catalog.nameEn : catalog.nameFr;
  }

  private loadPlacementOptions(afterLoad?: () => void): void {
    this.placementLoading.set(true);
    this.hospitalizationApi.placementOptions().pipe(finalize(() => this.placementLoading.set(false))).subscribe({
      next: (data) => {
        this.units.set(data.units);
        this.serviceCatalog.set(data.serviceCatalog);
        this.spaces.set(data.spaces);
        this.assignments.set(data.assignments);
        this.staffList.set(data.staff);
        afterLoad?.();
      },
      error: (error) => {
        const message = error.error?.detail || error.error?.title || this.t('patients.hospitalization.loadPlacementError');
        if (this.showTransferModal()) this.transferError.set(message);
        else this.admitError.set(message);
      },
    });
  }

  private loadFreeBedsForUnit(unitId: string): void {
    const request = ++this.bedRequest;
    this.bedsLoading.set(false);
    if (!unitId) {
      this.freeBeds.set([]);
      return;
    }
    const allowedSpaceIds = new Set(this.assignments()
      .filter((assignment) => assignment.organizationalUnitId === unitId)
      .map((assignment) => assignment.spaceId));
    const spaces = this.spaces().filter((space) =>
      space.active && space.inpatientProfile && allowedSpaceIds.has(space.id));
    if (spaces.length === 0) {
      this.freeBeds.set([]);
      return;
    }
    const requests = Object.fromEntries(spaces.map((space) => [space.id, this.hospitalizationApi.placementBeds(space.id)]));
    this.bedsLoading.set(true);
    forkJoin(requests).pipe(finalize(() => { if (request === this.bedRequest) this.bedsLoading.set(false); })).subscribe({
      next: (bedsBySpace) => {
        if (request !== this.bedRequest || unitId !== this.selectedUnitId()) return;
        const candidates: PlacementBed[] = [];
        for (const space of spaces) {
          const beds = (bedsBySpace[space.id] ?? []) as BedConfiguration[];
          for (const bed of beds) {
            if (isAdmissibleBed(bed)) candidates.push({ ...bed, roomNumber: space.name });
          }
        }
        this.freeBeds.set(candidates);
        this.selectedBedId.set('');
      },
      error: () => {
        if (request !== this.bedRequest || unitId !== this.selectedUnitId()) return;
        this.freeBeds.set([]);
        const message = this.t('patients.hospitalization.workflow.loadBedsError');
        if (this.showTransferModal()) this.transferError.set(message); else this.admitError.set(message);
      },
    });
  }

  openDischargeModal(): void {
    const active = this.activeHospitalization();
    if (!active || active.dischargeDecidedAt) return;
    this.dischargeDiagnosis = '';
    this.dischargeInstructions = '';
    this.againstMedicalAdvice = false;
    this.dischargeError.set(null);
    this.showDischargeModal.set(true);
  }

  saveDischarge(event: Event): void {
    event.preventDefault();
    const active = this.activeHospitalization();
    if (!active || active.dischargeDecidedAt || !this.dischargeDiagnosis.trim() || !this.dischargeInstructions.trim()) return;

    this.dischargeError.set(null);
    this.patientApi.dischargePatient(active.id, {
      dischargeDiagnosis: this.dischargeDiagnosis.trim(),
      dischargeInstructions: this.dischargeInstructions.trim(),
      againstMedicalAdvice: this.againstMedicalAdvice,
    }).subscribe({
      next: () => {
        this.showDischargeModal.set(false);
        this.loadHospitalizations();
      },
      error: (error) => this.dischargeError.set(
        error.error?.detail || error.error?.title || this.t('patients.hospitalization.dischargeError'),
      ),
    });
  }

  openPhysicalDepartureModal(): void {
    const active = this.activeHospitalization();
    if (!active?.dischargeDecidedAt || active.physicalDepartureAt) return;
    this.physicalDepartureNote = '';
    this.physicalDepartureError.set(null);
    this.showPhysicalDepartureModal.set(true);
  }

  savePhysicalDeparture(event: Event): void {
    event.preventDefault();
    const active = this.activeHospitalization();
    if (!active?.dischargeDecidedAt || active.physicalDepartureAt || this.physicalDepartureSubmitting()) return;

    this.physicalDepartureError.set(null);
    this.physicalDepartureSubmitting.set(true);
    this.patientApi.confirmPhysicalDeparture(active.id, {
      confirmed: true,
      note: this.physicalDepartureNote.trim() || undefined,
    }).subscribe({
      next: () => {
        this.physicalDepartureSubmitting.set(false);
        this.showPhysicalDepartureModal.set(false);
        this.loadHospitalizations();
      },
      error: (error) => {
        this.physicalDepartureSubmitting.set(false);
        this.physicalDepartureError.set(error.error?.detail || error.error?.title || this.t('patients.hospitalization.physicalDepartureError'));
      },
    });
  }

  downloadDischargePdf(hospitalization: StructuredHospitalization): void {
    this.patientApi.downloadDischargePdf(hospitalization.id).subscribe({
      next: (blob) => this.downloadBlob(blob, `fiche-sortie-${hospitalization.id}.pdf`),
      error: (error) => {
        this.actionError.set(this.t('patients.hospitalization.workflow.downloadError'));
      },
    });
  }

  downloadEntryPdf(hospitalization: StructuredHospitalization): void {
    this.patientApi.downloadEntryPdf(hospitalization.id).subscribe({
      next: (blob) => this.downloadBlob(blob, `billet-entree-${hospitalization.id}.pdf`),
      error: (error) => {
        this.actionError.set(this.t('patients.hospitalization.workflow.downloadError'));
      },
    });
  }

  private downloadBlob(blob: Blob, filename: string): void {
    const url = window.URL.createObjectURL(blob);
    const anchor = document.createElement('a');
    anchor.href = url;
    anchor.download = filename;
    document.body.appendChild(anchor);
    anchor.click();
    document.body.removeChild(anchor);
    window.URL.revokeObjectURL(url);
  }

}
