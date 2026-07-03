import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { AuthTokenStorageService } from '../auth/auth-token-storage.service';
import { Router, RouterLink } from '@angular/router';
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
  private readonly router = inject(Router);

  readonly session = this.tokenStorage.session;
  readonly welcomeLabel = computed(() => this.i18n.t('dashboard.welcome'));
  readonly authorizedLabel = computed(() => this.i18n.t('dashboard.authorized'));
  readonly roleLabel = computed(() => this.i18n.t('dashboard.role'));

  activeVisits = signal<Visit[]>([]);
  isLoadingQueue = signal(false);
  queueError = signal('');

  // Drawer state
  showVisitDrawer = signal(false);
  selectedVisitForDrawer = signal<Visit | null>(null);

  // Close confirmation modal state
  showCloseConfirmModal = signal(false);
  visitIdToClose = signal<string | null>(null);
  isClosingVisit = signal(false);
  closeVisitError = signal<string | null>(null);

  // Vitals entry modal state
  showVitalsModal = signal(false);
  selectedVisitForVitals = signal<Visit | null>(null);
  isSavingVitals = signal(false);
  vitalsError = signal('');

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
    this.isLoadingQueue.set(true);
    this.queueError.set('');
    this.visitApi.getActiveVisits().subscribe({
      next: (data) => {
        this.activeVisits.set([...data].sort((a, b) =>
          new Date(a.createdAt).getTime() - new Date(b.createdAt).getTime()
        ));
        this.isLoadingQueue.set(false);

        // Update selected visit in the drawer if it's currently open
        const currentDrawerVisit = this.selectedVisitForDrawer();
        if (currentDrawerVisit) {
          const updated = data.find(v => v.id === currentDrawerVisit.id);
          if (updated) {
            this.selectedVisitForDrawer.set(updated);
          } else {
            this.closeVisitDrawer();
          }
        }
      },
      error: (err) => {
        this.isLoadingQueue.set(false);
        this.queueError.set(err.error?.detail || 'Impossible de charger la file d\'attente active.');
      }
    });
  }

  openVisitDrawer(visit: Visit): void {
    this.selectedVisitForDrawer.set(visit);
    this.showVisitDrawer.set(true);
  }

  closeVisitDrawer(): void {
    this.showVisitDrawer.set(false);
    this.selectedVisitForDrawer.set(null);
  }

  openCloseConfirmModal(visitId: string): void {
    this.visitIdToClose.set(visitId);
    this.closeVisitError.set(null);
    this.showCloseConfirmModal.set(true);
  }

  cancelCloseConfirm(): void {
    if (!this.isClosingVisit()) {
      this.showCloseConfirmModal.set(false);
      this.visitIdToClose.set(null);
    }
  }

  confirmCloseVisit(): void {
    const visitId = this.visitIdToClose();
    if (!visitId || this.isClosingVisit()) return;

    this.isClosingVisit.set(true);
    this.closeVisitError.set(null);

    this.visitApi.closeVisit(visitId).subscribe({
      next: () => {
        this.isClosingVisit.set(false);
        this.showCloseConfirmModal.set(false);
        this.visitIdToClose.set(null);
        const currentDrawerVisit = this.selectedVisitForDrawer();
        if (currentDrawerVisit && currentDrawerVisit.id === visitId) {
          this.closeVisitDrawer();
        }
        this.loadQueue();
      },
      error: (err) => {
        this.isClosingVisit.set(false);
        this.closeVisitError.set(err.error?.detail || 'Erreur lors de la clôture de la visite.');
      }
    });
  }

  t(key: string): string {
    return this.i18n.t(key);
  }

  startConsultation(visitId: string): void {
    this.router.navigate(['/clinic/consultation', visitId]);
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

  isTempInvalid(): boolean {
    return this.vitalsTemp !== undefined && this.vitalsTemp !== null && (this.vitalsTemp < 30 || this.vitalsTemp > 45);
  }
  isWeightInvalid(): boolean {
    return this.vitalsWeight !== undefined && this.vitalsWeight !== null && (this.vitalsWeight < 1 || this.vitalsWeight > 500);
  }
  isHeightInvalid(): boolean {
    return this.vitalsHeight !== undefined && this.vitalsHeight !== null && (this.vitalsHeight < 30 || this.vitalsHeight > 250);
  }
  isPulseInvalid(): boolean {
    return this.vitalsPulse !== undefined && this.vitalsPulse !== null && (this.vitalsPulse < 20 || this.vitalsPulse > 250);
  }
  isSystolicInvalid(): boolean {
    return this.vitalsSystolic !== undefined && this.vitalsSystolic !== null && (this.vitalsSystolic < 40 || this.vitalsSystolic > 250);
  }
  isDiastolicInvalid(): boolean {
    return this.vitalsDiastolic !== undefined && this.vitalsDiastolic !== null && (this.vitalsDiastolic < 30 || this.vitalsDiastolic > 150);
  }
  isSpo2Invalid(): boolean {
    return this.vitalsSpo2 !== undefined && this.vitalsSpo2 !== null && (this.vitalsSpo2 < 50 || this.vitalsSpo2 > 100);
  }
  isGlycemiaInvalid(): boolean {
    return this.vitalsGlycemia !== undefined && this.vitalsGlycemia !== null && (this.vitalsGlycemia < 0.1 || this.vitalsGlycemia > 10.0);
  }
  isRespInvalid(): boolean {
    return this.vitalsResp !== undefined && this.vitalsResp !== null && (this.vitalsResp < 5 || this.vitalsResp > 100);
  }

  isAnyVitalInvalid(): boolean {
    return this.isTempInvalid() || this.isWeightInvalid() || this.isHeightInvalid() ||
        this.isPulseInvalid() || this.isSystolicInvalid() || this.isDiastolicInvalid() ||
        this.isSpo2Invalid() || this.isGlycemiaInvalid() || this.isRespInvalid();
  }

  openVitalsModal(visit: Visit): void {
    this.selectedVisitForVitals.set(visit);
    this.vitalsError.set('');
    this.isSavingVitals.set(false);

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

    this.showVitalsModal.set(true);
  }

  closeVitalsModal(): void {
    if (!this.isSavingVitals()) {
      this.showVitalsModal.set(false);
      this.selectedVisitForVitals.set(null);
    }
  }

  submitVitals(): void {
    const selectedVisit = this.selectedVisitForVitals();
    if (!selectedVisit || this.isSavingVitals()) return;

    this.isSavingVitals.set(true);
    this.vitalsError.set('');

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

    this.visitApi.saveVitals(selectedVisit.id, payload).subscribe({
      next: () => {
        this.isSavingVitals.set(false);
        this.showVitalsModal.set(false);
        this.selectedVisitForVitals.set(null);
        this.loadQueue();
      },
      error: (err) => {
        this.isSavingVitals.set(false);
        this.vitalsError.set(err.error?.detail || err.error?.title || 'Une erreur est survenue lors de l\'enregistrement des constantes.');
      }
    });
  }
}
