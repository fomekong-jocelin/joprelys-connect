import { Component, computed, inject, OnInit } from '@angular/core';
import { AuthTokenStorageService } from '../auth/auth-token-storage.service';
import { RouterLink } from '@angular/router';
import { I18nService } from '../core/i18n/i18n.service';
import { AppShellComponent } from '../shared/layout/app-shell.component';
import { VisitApiService } from '../visit/visit-api.service';
import { Visit } from '../visit/visit.models';
import { DatePipe } from '@angular/common';
import { EmptyStateComponent } from '../shared/ui/empty-state.component';
import { ButtonComponent } from '../shared/ui/button.component';
import { FormsModule } from '@angular/forms';

@Component({
  selector: 'app-dashboard',
  templateUrl: './dashboard.component.html',
  imports: [AppShellComponent, RouterLink, DatePipe, EmptyStateComponent, ButtonComponent, FormsModule]
})
export class DashboardComponent implements OnInit {
  private readonly tokenStorage = inject(AuthTokenStorageService);
  private readonly i18n = inject(I18nService);
  private readonly visitApi = inject(VisitApiService);

  readonly session = this.tokenStorage.session;
  readonly welcomeLabel = computed(() => this.i18n.t('dashboard.welcome'));
  readonly authorizedLabel = computed(() => this.i18n.t('dashboard.authorized'));
  readonly roleLabel = computed(() => this.i18n.t('dashboard.role'));

  activeVisits: Visit[] = [];
  isLoadingQueue = false;
  queueError = '';

  // Vitals entry modal state
  showVitalsModal = false;
  selectedVisitForVitals: Visit | null = null;
  isSavingVitals = false;
  vitalsError = '';

  vitalsTemp?: number;
  vitalsWeight?: number;
  vitalsHeight?: number;
  vitalsPulse?: number;
  vitalsSystolic?: number;
  vitalsDiastolic?: number;
  vitalsSpo2?: number;
  vitalsGlycemia?: number;
  vitalsResp?: number;

  readonly isClinicalRole = computed(() => {
    const role = this.session()?.role;
    return role === 'MEDECIN' || role === 'AGENT_ACCUEIL' || role === 'INFIRMIER' || role === 'ADMIN_CLINIQUE';
  });

  readonly canCloseVisit = computed(() => {
    const role = this.session()?.role;
    return role === 'MEDECIN' || role === 'ADMIN_CLINIQUE';
  });

  ngOnInit(): void {
    if (this.isClinicalRole()) {
      this.loadQueue();
    }
  }

  loadQueue(): void {
    this.isLoadingQueue = true;
    this.queueError = '';
    this.visitApi.getActiveVisits().subscribe({
      next: (data) => {
        this.activeVisits = data;
        this.isLoadingQueue = false;
      },
      error: (err) => {
        this.isLoadingQueue = false;
        this.queueError = 'Impossible de charger la file d\'attente active.';
      }
    });
  }

  closeVisit(visitId: string): void {
    if (!confirm('Voulez-vous vraiment clôturer cette visite ?')) {
      return;
    }

    this.visitApi.closeVisit(visitId).subscribe({
      next: () => {
        this.loadQueue();
      },
      error: (err) => {
        alert(err.error?.detail || 'Erreur lors de la clôtures de la visite.');
      }
    });
  }

  t(key: string): string {
    return this.i18n.t(key);
  }

  get computedBmi(): number | null {
    if (!this.vitalsWeight || !this.vitalsHeight || this.vitalsHeight <= 0) {
      return null;
    }
    const heightM = this.vitalsHeight / 100;
    return parseFloat((this.vitalsWeight / (heightM * heightM)).toFixed(2));
  }

  getBmiClass(bmi?: number): string {
    if (!bmi) return 'bg-slate-50 text-slate-600 dark:bg-slate-900 dark:text-slate-400';
    if (bmi < 18.5) return 'bg-amber-50 text-amber-700 dark:bg-amber-950/25 dark:text-amber-300';
    if (bmi < 25) return 'bg-emerald-50 text-emerald-700 dark:bg-emerald-950/25 dark:text-emerald-300';
    if (bmi < 30) return 'bg-yellow-50 text-yellow-700 dark:bg-yellow-950/25 dark:text-yellow-300';
    return 'bg-red-50 text-red-700 dark:bg-red-950/25 dark:text-red-300';
  }

  openVitalsModal(visit: Visit): void {
    this.selectedVisitForVitals = visit;
    this.vitalsError = '';
    this.isSavingVitals = false;

    if (visit.vitals) {
      this.vitalsTemp = visit.vitals.temperature;
      this.vitalsWeight = visit.vitals.weight;
      this.vitalsHeight = visit.vitals.height;
      this.vitalsPulse = visit.vitals.pulse;
      this.vitalsSystolic = visit.vitals.systolic;
      this.vitalsDiastolic = visit.vitals.diastolic;
      this.vitalsSpo2 = visit.vitals.spo2;
      this.vitalsGlycemia = visit.vitals.glycemia;
      this.vitalsResp = visit.vitals.respiratoryRate;
    } else {
      this.vitalsTemp = undefined;
      this.vitalsWeight = undefined;
      this.vitalsHeight = undefined;
      this.vitalsPulse = undefined;
      this.vitalsSystolic = undefined;
      this.vitalsDiastolic = undefined;
      this.vitalsSpo2 = undefined;
      this.vitalsGlycemia = undefined;
      this.vitalsResp = undefined;
    }

    this.showVitalsModal = true;
  }

  closeVitalsModal(): void {
    if (!this.isSavingVitals) {
      this.showVitalsModal = false;
      this.selectedVisitForVitals = null;
    }
  }

  submitVitals(): void {
    if (!this.selectedVisitForVitals || this.isSavingVitals) return;

    this.isSavingVitals = true;
    this.vitalsError = '';

    const payload = {
      temperature: this.vitalsTemp,
      weight: this.vitalsWeight,
      height: this.vitalsHeight,
      pulse: this.vitalsPulse,
      systolic: this.vitalsSystolic,
      diastolic: this.vitalsDiastolic,
      spo2: this.vitalsSpo2,
      glycemia: this.vitalsGlycemia,
      respiratoryRate: this.vitalsResp
    };

    this.visitApi.saveVitals(this.selectedVisitForVitals.id, payload).subscribe({
      next: () => {
        this.isSavingVitals = false;
        this.showVitalsModal = false;
        this.selectedVisitForVitals = null;
        this.loadQueue();
      },
      error: (err) => {
        this.isSavingVitals = false;
        this.vitalsError = err.error?.detail || err.error?.title || 'Une erreur est survenue lors de l\'enregistrement des constantes.';
      }
    });
  }
}
