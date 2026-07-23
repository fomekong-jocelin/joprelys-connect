import { CommonModule, DatePipe } from '@angular/common';
import { Component, Input, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { forkJoin } from 'rxjs';
import { HospitalOrganizationApiService } from '../clinic/hospital-organization/hospital-organization-api.service';
import { HospitalServiceCatalogEntry, OrganizationalUnit } from '../clinic/hospital-organization/hospital-organization.models';
import { RbacApiService } from '../clinic/rbac/rbac-api.service';
import { StaffApiService } from '../clinic/staff/staff-api.service';
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
  private readonly hospitalOrganizationApi = inject(HospitalOrganizationApiService);
  private readonly rbacApi = inject(RbacApiService);
  private readonly i18n = inject(I18nService);
  private readonly staffApi = inject(StaffApiService);

  readonly t = (key: string, defaultValue?: string) => this.i18n.t(key, defaultValue);

  readonly list = signal<StructuredHospitalization[]>([]);
  readonly staffList = signal<any[]>([]);
  readonly patientVisits = signal<any[]>([]);

  readonly showAdmitModal = signal(false);
  readonly showDischargeModal = signal(false);
  readonly showPhysicalDepartureModal = signal(false);
  readonly showTransferModal = signal(false);
  readonly admitError = signal<string | null>(null);
  readonly transferError = signal<string | null>(null);
  readonly transferSuccessMsg = signal<string | null>(null);
  readonly dischargeError = signal<string | null>(null);
  readonly physicalDepartureError = signal<string | null>(null);
  readonly physicalDepartureSubmitting = signal(false);

  readonly consents = signal<any[]>([]);
  readonly showAddConsent = signal(false);
  consentType = 'ANESTHESIA';
  patientSignaturePresent = false;
  witnessName = '';
  consentFile: File | null = null;

  activeTab = 'notes';

  readonly operatingReports = signal<any[]>([]);
  readonly showAddReport = signal(false);
  procedureName = '';
  procedureDescription = '';
  preOperativeDiagnosis = '';
  postOperativeDiagnosis = '';
  anesthesiaType = 'GÉNÉRALE';
  anesthesiaDescription = '';
  kSurgeonValue = 0.0;
  kAnesthesistValue = 0.0;
  kBlocValue = 0.0;
  surgeonId = '';
  anesthetistId = '';
  readonly implantsInForm = signal<any[]>([]);

  newImplantName = '';
  newImplantLot = '';
  newImplantQty = 1;
  newImplantPrice = 0.0;
  newImplantManufacturer = '';

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

  ngOnInit(): void {
    if (this.patientId) {
      this.loadHospitalizations();
      this.loadStaff();
      this.loadVisits();
    }
  }

  loadStaff(): void {
    this.staffApi.list().subscribe({ next: (data) => this.staffList.set(data) });
  }

  loadVisits(): void {
    this.patientApi.getPatientVisits(this.patientId).subscribe({ next: (data) => this.patientVisits.set(data) });
  }

  loadHospitalizations(): void {
    this.hospitalizationApi.listForPatient(this.patientId).subscribe({
      next: (data) => {
        this.list.set(data);
        const active = this.activeHospitalization();
        if (active) {
          this.loadConsents(active.id);
          this.loadOperatingReports(active.id);
        }
      },
    });
  }

  loadConsents(hospitalizationId: string): void {
    this.patientApi.getConsents(hospitalizationId).subscribe({ next: (data) => this.consents.set(data) });
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
    if (!this.canAdmit()) return;
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
    this.loadFreeBedsForUnit(this.selectedUnitId());
  }

  onAdmissionBedChange(): void {
    const bed = this.freeBeds().find((candidate) => candidate.id === this.selectedBedId());
    this.selectedSpaceId.set(bed?.spaceId ?? '');
  }

  saveAdmission(event: Event): void {
    event.preventDefault();
    this.onAdmissionBedChange();
    if (!this.canAdmit()
      || !this.selectedUnitId()
      || !this.selectedSpaceId()
      || !this.selectedBedId()
      || !this.admissionReason.trim()
      || !this.visitId
      || !this.responsiblePractitionerId) return;

    this.admitError.set(null);
    this.hospitalizationApi.admit({
      patientId: this.patientId,
      serviceUnitId: this.selectedUnitId(),
      spaceId: this.selectedSpaceId(),
      bedId: this.selectedBedId(),
      admissionReason: this.admissionReason.trim(),
      visitId: this.visitId,
      responsiblePractitionerId: this.responsiblePractitionerId,
    }).subscribe({
      next: () => {
        this.showAdmitModal.set(false);
        this.loadHospitalizations();
      },
      error: (error) => this.admitError.set(
        error.error?.detail || error.error?.title || this.t('patients.hospitalization.bedOccupied', 'Le lit sélectionné n’est plus disponible.'),
      ),
    });
  }

  saveTransfer(event: Event): void {
    event.preventDefault();
    const active = this.activeHospitalization();
    const bed = this.freeBeds().find((candidate) => candidate.id === this.selectedBedId());
    if (bed) this.selectedSpaceId.set(bed.spaceId);
    if (!active
      || active.dischargeDecidedAt
      || !this.selectedUnitId()
      || !this.selectedSpaceId()
      || !this.selectedBedId()) return;

    this.transferError.set(null);
    this.transferSuccessMsg.set(null);
    this.spatialApi.transferPatient({
      hospitalizationId: active.id,
      targetServiceUnitId: this.selectedUnitId(),
      targetSpaceId: this.selectedSpaceId(),
      targetBedId: this.selectedBedId(),
    }).subscribe({
      next: () => {
        this.transferSuccessMsg.set(this.t('patients.hospitalization.transferSuccess', 'Transfert effectué.'));
        setTimeout(() => {
          this.showTransferModal.set(false);
          this.loadHospitalizations();
        }, 800);
      },
      error: (error) => this.transferError.set(
        error.error?.detail || error.error?.title || this.t('patients.hospitalization.transferError', 'Le transfert n’a pas pu être effectué.'),
      ),
    });
  }

  unitLabel(unit: OrganizationalUnit): string {
    if (unit.name) return unit.name;
    const catalog = this.serviceCatalog().find((entry) => entry.code === unit.serviceCatalogCode);
    if (!catalog) return unit.code;
    return this.i18n.currentLanguage() === 'en' ? catalog.nameEn : catalog.nameFr;
  }

  private loadPlacementOptions(afterLoad?: () => void): void {
    forkJoin({
      units: this.hospitalOrganizationApi.listUnits(undefined, false),
      serviceCatalog: this.hospitalOrganizationApi.listServiceCatalog(),
      spaces: this.spatialApi.listSpaces(undefined, undefined, false),
      assignments: this.spatialApi.listUnitSpaceAssignments(undefined, { activeAt: new Date().toISOString() }),
    }).subscribe({
      next: (data) => {
        this.units.set(data.units);
        this.serviceCatalog.set(data.serviceCatalog);
        this.spaces.set(data.spaces);
        this.assignments.set(data.assignments);
        afterLoad?.();
      },
      error: (error) => {
        const message = error.error?.detail || error.error?.title || this.t('patients.hospitalization.loadPlacementError', 'Impossible de charger les services et espaces disponibles.');
        if (this.showTransferModal()) this.transferError.set(message);
        else this.admitError.set(message);
      },
    });
  }

  private loadFreeBedsForUnit(unitId: string): void {
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
    const requests = Object.fromEntries(spaces.map((space) => [space.id, this.spatialApi.listBeds(space.id)]));
    forkJoin(requests).subscribe({
      next: (bedsBySpace) => {
        const candidates: PlacementBed[] = [];
        for (const space of spaces) {
          const beds = (bedsBySpace[space.id] ?? []) as BedConfiguration[];
          for (const bed of beds) {
            if (bed.available) candidates.push({ ...bed, roomNumber: space.name });
          }
        }
        this.freeBeds.set(candidates);
        this.selectedBedId.set('');
      },
      error: () => this.freeBeds.set([]),
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
        error.error?.detail || error.error?.title || this.t('patients.hospitalization.dischargeError', 'Impossible d’enregistrer la décision médicale de sortie.'),
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
        this.physicalDepartureError.set(error.error?.detail || error.error?.title || this.t('patients.hospitalization.physicalDepartureError', 'Impossible de confirmer le départ physique.'));
      },
    });
  }

  downloadDischargePdf(hospitalization: StructuredHospitalization): void {
    this.patientApi.downloadDischargePdf(hospitalization.id).subscribe({
      next: (blob) => this.downloadBlob(blob, `fiche-sortie-${hospitalization.id}.pdf`),
      error: (error) => {
        console.error('Error downloading pdf', error);
        alert('Erreur lors du téléchargement du PDF');
      },
    });
  }

  downloadEntryPdf(hospitalization: StructuredHospitalization): void {
    this.patientApi.downloadEntryPdf(hospitalization.id).subscribe({
      next: (blob) => this.downloadBlob(blob, `billet-entree-${hospitalization.id}.pdf`),
      error: (error) => {
        console.error('Error downloading entry pdf', error);
        alert('Erreur lors du téléchargement du billet d\'entrée');
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

  onConsentFileChange(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.consentFile = input.files && input.files.length > 0 ? input.files[0] : null;
  }

  saveConsent(event: Event): void {
    event.preventDefault();
    const active = this.activeHospitalization();
    if (!active || !this.canRecordConsent()) return;

    const formData = new FormData();
    formData.append('consentType', this.consentType);
    formData.append('patientSignaturePresent', String(this.patientSignaturePresent));
    formData.append('witnessName', this.witnessName.trim());
    if (this.consentFile) formData.append('file', this.consentFile);

    this.patientApi.addConsent(active.id, formData).subscribe({
      next: () => {
        this.showAddConsent.set(false);
        this.consentFile = null;
        this.witnessName = '';
        this.patientSignaturePresent = false;
        this.loadConsents(active.id);
      },
      error: (error) => {
        console.error('Error saving consent', error);
        alert('Erreur lors de l\'enregistrement du consentement');
      },
    });
  }

  downloadConsentFile(documentId: string): void {
    window.open(`/api/documents/${documentId}/download`, '_blank');
  }

  loadOperatingReports(hospitalizationId: string): void {
    this.patientApi.getOperatingReports(hospitalizationId).subscribe({ next: (data) => this.operatingReports.set(data) });
  }

  addImplantToList(): void {
    if (!this.newImplantName.trim()) return;
    this.implantsInForm.update((list) => [...list, {
      implantName: this.newImplantName.trim(),
      lotNumber: this.newImplantLot.trim(),
      quantity: this.newImplantQty,
      unitPrice: this.newImplantPrice,
      manufacturer: this.newImplantManufacturer.trim(),
    }]);
    this.newImplantName = '';
    this.newImplantLot = '';
    this.newImplantQty = 1;
    this.newImplantPrice = 0.0;
    this.newImplantManufacturer = '';
  }

  removeImplantFromList(index: number): void {
    this.implantsInForm.update((list) => list.filter((_, itemIndex) => itemIndex !== index));
  }

  saveOperatingReport(event: Event): void {
    event.preventDefault();
    const active = this.activeHospitalization();
    if (!active || !this.canWriteOperatingReport() || !this.procedureName.trim()) return;

    this.patientApi.createOperatingReport(active.id, {
      procedureName: this.procedureName.trim(),
      procedureDescription: this.procedureDescription.trim(),
      preOperativeDiagnosis: this.preOperativeDiagnosis.trim(),
      postOperativeDiagnosis: this.postOperativeDiagnosis.trim(),
      anesthesiaType: this.anesthesiaType.trim(),
      anesthesiaDescription: this.anesthesiaDescription.trim(),
      kSurgeonValue: this.kSurgeonValue,
      kAnesthesistValue: this.kAnesthesistValue,
      kBlocValue: this.kBlocValue,
      surgeonId: this.surgeonId || null,
      anesthetistId: this.anesthetistId || null,
      implants: this.implantsInForm(),
    }).subscribe({
      next: () => {
        this.showAddReport.set(false);
        this.procedureName = '';
        this.procedureDescription = '';
        this.preOperativeDiagnosis = '';
        this.postOperativeDiagnosis = '';
        this.anesthesiaType = 'GÉNÉRALE';
        this.anesthesiaDescription = '';
        this.kSurgeonValue = 0.0;
        this.kAnesthesistValue = 0.0;
        this.kBlocValue = 0.0;
        this.surgeonId = '';
        this.anesthetistId = '';
        this.implantsInForm.set([]);
        this.loadOperatingReports(active.id);
      },
      error: (error) => {
        console.error('Error saving operating report', error);
        alert('Erreur lors de l\'enregistrement du compte-rendu opératoire');
      },
    });
  }

  validateReport(reportId: string): void {
    if (!this.canWriteOperatingReport()) return;
    if (!confirm('Êtes-vous sûr de vouloir valider ce compte-rendu opératoire ? Cette action le rendra immuable et générera les actes de facturation.')) return;

    this.patientApi.validateOperatingReport(reportId).subscribe({
      next: () => {
        const active = this.activeHospitalization();
        if (active) this.loadOperatingReports(active.id);
      },
      error: (error) => {
        console.error('Error validating report', error);
        alert('Erreur lors de la validation du compte-rendu opératoire');
      },
    });
  }
}
