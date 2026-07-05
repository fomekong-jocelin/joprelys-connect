import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { DatePipe, DecimalPipe, NgClass } from '@angular/common';
import { AlertComponent } from '../../shared/ui/alert.component';
import { ButtonComponent } from '../../shared/ui/button.component';
import { CardComponent } from '../../shared/ui/card.component';
import { PatientApiService } from '../../patient/patient-api.service';
import { Patient, PatientDuplicateCandidate } from '../../patient/patient.models';
import { I18nService } from '../../core/i18n/i18n.service';

@Component({
  selector: 'app-duplicates-page',
  standalone: true,
  imports: [
    AlertComponent,
    ButtonComponent,
    CardComponent,
    DatePipe,
    DecimalPipe,
    NgClass
  ],
  templateUrl: './duplicates-page.component.html'
})
export class DuplicatesPageComponent implements OnInit {
  private readonly patientApi = inject(PatientApiService);
  private readonly i18n = inject(I18nService);

  candidates = signal<PatientDuplicateCandidate[]>([]);
  isLoading = signal(false);
  error = signal<string | null>(null);

  // Merge Assistant Modal State
  selectedCandidate = signal<PatientDuplicateCandidate | null>(null);
  selectedPrimaryId = signal<string | null>(null);
  isProcessing = signal(false);
  actionError = signal<string | null>(null);

  ngOnInit(): void {
    this.loadDuplicates();
  }

  loadDuplicates(): void {
    this.isLoading.set(true);
    this.error.set(null);
    this.patientApi.getDuplicates().subscribe({
      next: (data) => {
        this.candidates.set(data);
        this.isLoading.set(false);
      },
      error: (err) => {
        this.isLoading.set(false);
        this.error.set(this.t('duplicates.loadError'));
      }
    });
  }

  t(key: string): string {
    return this.i18n.t(key);
  }

  openMergeAssistant(candidate: PatientDuplicateCandidate): void {
    this.selectedCandidate.set(candidate);
    // By default, sourcePatient is primary
    this.selectedPrimaryId.set(candidate.sourcePatient.id);
    this.actionError.set(null);
  }

  closeMergeAssistant(): void {
    if (!this.isProcessing()) {
      this.selectedCandidate.set(null);
      this.selectedPrimaryId.set(null);
    }
  }

  selectPrimary(id: string): void {
    this.selectedPrimaryId.set(id);
  }

  ignoreCandidate(candidate: PatientDuplicateCandidate): void {
    if (this.isProcessing()) return;
    this.isProcessing.set(true);
    this.actionError.set(null);

    this.patientApi.ignoreDuplicate(candidate.id).subscribe({
      next: () => {
        this.isProcessing.set(false);
        this.loadDuplicates();
      },
      error: (err) => {
        this.isProcessing.set(false);
        this.actionError.set(err.error?.detail || 'Erreur lors du rejet du candidat.');
      }
    });
  }

  confirmMerge(): void {
    const candidate = this.selectedCandidate();
    const primaryId = this.selectedPrimaryId();
    if (!candidate || !primaryId || this.isProcessing()) return;

    const secondaryId = candidate.sourcePatient.id === primaryId 
      ? candidate.targetPatient.id 
      : candidate.sourcePatient.id;

    this.isProcessing.set(true);
    this.actionError.set(null);

    this.patientApi.mergePatients(primaryId, secondaryId).subscribe({
      next: () => {
        this.isProcessing.set(false);
        this.selectedCandidate.set(null);
        this.selectedPrimaryId.set(null);
        this.loadDuplicates();
      },
      error: (err) => {
        this.isProcessing.set(false);
        this.actionError.set(err.error?.detail || 'Erreur lors de la fusion des dossiers.');
      }
    });
  }

  hasDiff(field: keyof Patient): boolean {
    const candidate = this.selectedCandidate();
    if (!candidate) return false;
    const valA = candidate.sourcePatient[field];
    const valB = candidate.targetPatient[field];
    if (valA === undefined && valB === undefined) return false;
    return String(valA || '').trim().toLowerCase() !== String(valB || '').trim().toLowerCase();
  }
}