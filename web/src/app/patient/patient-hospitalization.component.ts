import { CommonModule, DatePipe } from '@angular/common';
import { Component, Input, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RbacApiService } from '../clinic/rbac/rbac-api.service';
import { StaffApiService } from '../clinic/staff/staff-api.service';
import { I18nService } from '../core/i18n/i18n.service';
import { HospitalizationConsumptionPanelComponent } from './hospitalization-consumption-panel.component';
import { HospitalizationDailyCarePanelComponent } from './hospitalization-daily-care-panel.component';
import { HospitalizationMedicationPanelComponent } from './hospitalization-medication-panel.component';
import { HospitalizationNotesPanelComponent } from './hospitalization-notes-panel.component';
import { HospitalizationStayHeaderComponent } from './hospitalization-stay-header.component';
import { PatientApiService } from './patient-api.service';
import { Hospitalization, Ward } from './patient.models';
import { SpatialApiService } from './spatial-api.service';

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
  private readonly spatialApi = inject(SpatialApiService);
  private readonly rbacApi = inject(RbacApiService);
  private readonly i18n = inject(I18nService);
  private readonly staffApi = inject(StaffApiService);

  readonly t = (key: string, defaultValue?: string) => this.i18n.t(key, defaultValue);

  readonly list = signal<Hospitalization[]>([]);
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
  kSurgeonValue = 0;
  kAnesthesistValue = 0;
  kBlocValue = 0;
  surgeonId = '';
  anesthetistId = '';
  readonly implantsInForm = signal<any[]>([]);

  newImplantName = '';
  newImplantLot = '';
  newImplantQty = 1;
  newImplantPrice = 0;
  newImplantManufacturer = '';

  readonly wards = signal<Ward[]>([]);
  readonly freeBeds = signal<{ id: string; roomNumber: string; bedNumber: string }[]>([]);
  readonly selectedWardId = signal('');
  readonly selectedBedId = signal('');

  serviceName = 'MÉDECINE GÉNÉRALE';
  roomNumber = '';
  bedNumber = '';
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

  readonly staffMap = computed(() => {
    const map = new Map<string, string>();
    for (const staff of this.staffList()) {
      map.set(staff.id, staff.displayName);
    }
    return map;
  });

  ngOnInit(): void {
    if (!this.patientId) return;
    this.loadHospitalizations();
    this.loadStaff();
    this.loadVisits();
  }

  loadStaff(): void {
    this.staffApi.list().subscribe({
      next: (data) => this.staffList.set(data),
    });
  }

  loadVisits(): void {
    this.patientApi.getPatientVisits(this.patientId).subscribe({
      next: (data) => this.patientVisits.set(data),
    });
  }

  loadHospitalizations(): void {
    this.patientApi.getHospitalizations(this.patientId).subscribe({
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
    this.patientApi.getConsents(hospitalizationId).subscribe({
      next: (data) => this.consents.set(data),
    });
  }

  hasRole(roleStr: string | undefined, allowedRoles: string[] | string): boolean {
    if (!roleStr) return false;
    const roles = roleStr.split(',').map((role) => role.trim());
    return Array.isArray(allowedRoles)
      ? roles.some((role) => allowedRoles.includes(role))
      : roles.includes(allowedRoles);
  }

  canAdmit(): boolean {
    return this.rbacApi.hasPermission('HOSPITALIZATION_ADMIT');
  }

  canWriteNotes(): boolean {
    return this.rbacApi.hasPermission('HOSPITALIZATION_NOTE_WRITE');
  }

  canRecordConsent(): boolean {
    return this.rbacApi.hasPermission('HOSPITALIZATION_CONSENT_RECORD');
  }

  canRecordCare(): boolean {
    return this.rbacApi.hasPermission('HOSPITALIZATION_CARE_RECORD');
  }

  canAdministerMedication(): boolean {
    return this.rbacApi.hasPermission('HOSPITALIZATION_MEDICATION_ADMINISTER');
  }

  canRecordConsumable(): boolean {
    return this.rbacApi.hasPermission('HOSPITALIZATION_CONSUMABLE_RECORD');
  }

  canManageOperatingReports(): boolean {
    return this.rbacApi.hasPermission('CLINICAL_WRITE');
  }

  /**
   * Compatibility hook retained for the existing template and child panels.
   * It resolves the exact permission for the currently displayed action area.
   */
  canModify(): boolean {
    if (!this.activeHospitalization()) {
      return this.canAdmit();
    }

    switch (this.activeTab) {
      case 'notes':
        return this.canWriteNotes();
      case 'consents':
        return this.canRecordConsent();
      case 'cares':
        return this.canRecordCare();
      case 'meds':
        return this.canAdministerMedication();
      case 'consumptions':
        return this.canRecordConsumable();
      case 'cro':
        return this.canManageOperatingReports();
      default:
        return false;
    }
  }

  loadWardsForAdmission(): void {
    this.spatialApi.listWards().subscribe({
      next: (data) => {
        this.wards.set(data);
        if (data.length === 0) return;
        const currentWard = data.find(
          (ward) => ward.name.toLowerCase() === this.serviceName.toLowerCase(),
        ) || data[0];
        this.selectedWardId.set(currentWard.id);
        this.serviceName = currentWard.name;
        this.loadFreeBedsForWard(currentWard.id);
      },
    });
  }

  loadFreeBedsForWard(wardId: string): void {
    if (!wardId) {
      this.freeBeds.set([]);
      return;
    }

    this.spatialApi.getWardOccupancy(wardId).subscribe({
      next: (occupancy) => {
        const beds: { id: string; roomNumber: string; bedNumber: string }[] = [];
        for (const room of occupancy.rooms) {
          for (const bed of room.beds) {
            if (bed.status === 'FREE') {
              beds.push({
                id: bed.id,
                roomNumber: room.roomNumber,
                bedNumber: bed.bedNumber,
              });
            }
          }
        }
        this.freeBeds.set(beds);
        this.selectedBedId.set('');
        this.roomNumber = '';
        this.bedNumber = '';
      },
    });
  }

  onAdmissionWardChange(): void {
    const ward = this.wards().find((candidate) => candidate.id === this.selectedWardId());
    if (!ward) return;
    this.serviceName = ward.name;
    this.loadFreeBedsForWard(ward.id);
  }

  onAdmissionBedChange(): void {
    const bed = this.freeBeds().find((candidate) => candidate.id === this.selectedBedId());
    this.roomNumber = bed?.roomNumber ?? '';
    this.bedNumber = bed?.bedNumber ?? '';
  }

  openAdmitModal(): void {
    if (!this.canAdmit()) return;
    this.roomNumber = '';
    this.bedNumber = '';
    this.admissionReason = '';
    this.visitId = '';
    this.responsiblePractitionerId = '';
    this.selectedWardId.set('');
    this.selectedBedId.set('');
    this.freeBeds.set([]);
    this.admitError.set(null);
    this.loadVisits();
    this.loadWardsForAdmission();
    this.showAdmitModal.set(true);
  }

  saveAdmission(event: Event): void {
    event.preventDefault();
    if (!this.canAdmit()) return;
    if (
      !this.roomNumber.trim()
      || !this.bedNumber.trim()
      || !this.admissionReason.trim()
      || !this.visitId
      || !this.responsiblePractitionerId
    ) return;

    this.admitError.set(null);
    this.patientApi.admitPatient({
      patientId: this.patientId,
      serviceName: this.serviceName,
      roomNumber: this.roomNumber.trim(),
      bedNumber: this.bedNumber.trim(),
      admissionReason: this.admissionReason.trim(),
      visitId: this.visitId,
      responsiblePractitionerId: this.responsiblePractitionerId,
    }).subscribe({
      next: () => {
        this.showAdmitModal.set(false);
        this.loadHospitalizations();
      },
      error: (error) => {
        this.admitError.set(
          error.error?.detail
          || error.error?.title
          || this.t('patients.hospitalization.bedOccupied'),
        );
      },
    });
  }

  openTransferModal(): void {
    const active = this.activeHospitalization();
    if (!active || active.dischargeDecidedAt) return;

    this.transferError.set(null);
    this.transferSuccessMsg.set(null);
    this.selectedWardId.set('');
    this.selectedBedId.set('');
    this.freeBeds.set([]);

    this.spatialApi.listWards().subscribe({
      next: (data) => {
        this.wards.set(data);
        if (data.length === 0) return;
        const currentWard = data.find(
          (ward) => ward.name.toLowerCase() === active.serviceName.toLowerCase(),
        ) || data[0];
        this.selectedWardId.set(currentWard.id);
        this.loadFreeBedsForWard(currentWard.id);
      },
    });
    this.showTransferModal.set(true);
  }

  saveTransfer(event: Event): void {
    event.preventDefault();
    const active = this.activeHospitalization();
    const bedId = this.selectedBedId();
    if (!active || active.dischargeDecidedAt || !bedId) return;

    this.transferError.set(null);
    this.transferSuccessMsg.set(null);
    this.spatialApi.transferPatient(active.id, bedId).subscribe({
      next: () => {
        this.transferSuccessMsg.set(this.t('patients.hospitalization.transferSuccess'));
        setTimeout(() => {
          this.showTransferModal.set(false);
          this.loadHospitalizations();
        }, 1500);
      },
      error: (error) => {
        this.transferError.set(
          error.error?.detail
          || error.error?.title
          || this.t('patients.hospitalization.transferError'),
        );
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
    if (
      !active
      || active.dischargeDecidedAt
      || !this.dischargeDiagnosis.trim()
      || !this.dischargeInstructions.trim()
    ) return;

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
      error: (error) => {
        this.dischargeError.set(
          error.error?.detail
          || error.error?.title
          || this.t(
            'patients.hospitalization.dischargeError',
            'Impossible d’enregistrer la décision médicale de sortie.',
          ),
        );
      },
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
    if (!active?.dischargeDecidedAt || active.physicalDepartureAt || this.physicalDepartureSubmitting()) {
      return;
    }

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
        this.physicalDepartureError.set(
          error.error?.detail
          || error.error?.title
          || this.t(
            'patients.hospitalization.physicalDepartureError',
            'Impossible de confirmer le départ physique.',
          ),
        );
      },
    });
  }

  downloadDischargePdf(hospitalization: Hospitalization): void {
    this.patientApi.downloadDischargePdf(hospitalization.id).subscribe({
      next: (blob) => this.downloadBlob(blob, `fiche-sortie-${hospitalization.id}.pdf`),
      error: (error) => {
        console.error('Error downloading pdf', error);
        alert('Erreur lors du téléchargement du PDF');
      },
    });
  }

  downloadEntryPdf(hospitalization: Hospitalization): void {
    this.patientApi.downloadEntryPdf(hospitalization.id).subscribe({
      next: (blob) => this.downloadBlob(blob, `billet-entree-${hospitalization.id}.pdf`),
      error: (error) => {
        console.error('Error downloading entry pdf', error);
        alert('Erreur lors du téléchargement du billet d\'entrée');
      },
    });
  }

  onConsentFileChange(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.consentFile = input.files?.[0] ?? null;
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
    this.patientApi.getOperatingReports(hospitalizationId).subscribe({
      next: (data) => this.operatingReports.set(data),
    });
  }

  addImplantToList(): void {
    if (!this.newImplantName.trim()) return;
    this.implantsInForm.update((implants) => [...implants, {
      implantName: this.newImplantName.trim(),
      lotNumber: this.newImplantLot.trim(),
      quantity: this.newImplantQty,
      unitPrice: this.newImplantPrice,
      manufacturer: this.newImplantManufacturer.trim(),
    }]);
    this.newImplantName = '';
    this.newImplantLot = '';
    this.newImplantQty = 1;
    this.newImplantPrice = 0;
    this.newImplantManufacturer = '';
  }

  removeImplantFromList(index: number): void {
    this.implantsInForm.update((implants) => implants.filter((_, currentIndex) => currentIndex !== index));
  }

  saveOperatingReport(event: Event): void {
    event.preventDefault();
    const active = this.activeHospitalization();
    if (!active || !this.procedureName.trim() || !this.canManageOperatingReports()) return;

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
        this.kSurgeonValue = 0;
        this.kAnesthesistValue = 0;
        this.kBlocValue = 0;
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
    if (!this.canManageOperatingReports()) return;
    if (!confirm('Êtes-vous sûr de vouloir valider ce compte-rendu opératoire ? Cette action le rendra immuable et générera les actes de facturation.')) {
      return;
    }

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
