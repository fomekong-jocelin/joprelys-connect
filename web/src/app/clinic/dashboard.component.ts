import { Component, OnInit, computed, inject, signal } from '@angular/core';
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
import { RbacApiService } from './rbac/rbac-api.service';
import { AiVitalField, AiVitalsProposal } from '../consultation/ai-vitals-api.service';
import { SmartVitalsAssistantComponent } from '../consultation/smart-vitals-assistant.component';

@Component({
  selector: 'app-dashboard',
  templateUrl: './dashboard.component.html',
  imports: [
    AppShellComponent,
    RouterLink,
    DatePipe,
    EmptyStateComponent,
    ButtonComponent,
    FormsModule,
    SmartVitalsAssistantComponent,
  ],
})
export class DashboardComponent implements OnInit {
  private readonly tokenStorage = inject(AuthTokenStorageService);
  private readonly i18n = inject(I18nService);
  private readonly visitApi = inject(VisitApiService);
  private readonly router = inject(Router);
  private readonly rbacApi = inject(RbacApiService);

  readonly session = this.tokenStorage.session;
  readonly welcomeLabel = computed(() => this.i18n.t('dashboard.welcome'));
  readonly authorizedLabel = computed(() => this.i18n.t('dashboard.authorized'));
  readonly roleLabel = computed(() => this.i18n.t('dashboard.role'));

  activeVisits = signal<Visit[]>([]);
  isLoadingQueue = signal(false);
  queueError = signal('');

  showVisitDrawer = signal(false);
  selectedVisitForDrawer = signal<Visit | null>(null);

  showCloseConfirmModal = signal(false);
  visitIdToClose = signal<string | null>(null);
  isClosingVisit = signal(false);
  closeVisitError = signal<string | null>(null);

  showVitalsModal = signal(false);
  selectedVisitForVitals = signal<Visit | null>(null);
  isSavingVitals = signal(false);
  vitalsError = signal('');

  showAuditSecurityModal = signal(false);

  vitalsTemp?: number;
  vitalsWeight?: number;
  vitalsHeight?: number;
  vitalsPulse?: number;
  vitalsSystolic?: number;
  vitalsDiastolic?: number;
  vitalsSpo2?: number;
  vitalsGlycemia?: number;
  vitalsResp?: number;
  vitalsPain?: number;

  hasPermission(permissions: string[] | string): boolean {
    const expected = Array.isArray(permissions) ? permissions : [permissions];
    return expected.some((permission) => this.rbacApi.hasPermission(permission));
  }

  readonly isClinicalRole = computed(() => this.hasPermission('VISIT_READ'));
  readonly canStartConsultation = computed(() => this.hasPermission('CLINICAL_WRITE'));
  readonly canCloseVisit = computed(() => this.hasPermission('VISIT_MANAGE'));

  ngOnInit(): void {
    if (this.isClinicalRole()) this.loadQueue();
  }

  loadQueue(): void {
    this.isLoadingQueue.set(true);
    this.queueError.set('');
    this.visitApi.getActiveVisits().subscribe({
      next: (data) => {
        this.activeVisits.set(
          [...data].sort(
            (a, b) => new Date(a.createdAt).getTime() - new Date(b.createdAt).getTime(),
          ),
        );
        this.isLoadingQueue.set(false);
        const currentDrawerVisit = this.selectedVisitForDrawer();
        if (currentDrawerVisit) {
          const updated = data.find((v) => v.id === currentDrawerVisit.id);
          if (updated) this.selectedVisitForDrawer.set(updated);
          else this.closeVisitDrawer();
        }
      },
      error: (err) => {
        this.isLoadingQueue.set(false);
        this.queueError.set(err.error?.detail || this.t('dashboard.queue.loadError'));
      },
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
    if (!this.canCloseVisit()) return;
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
    if (!this.canCloseVisit() || !visitId || this.isClosingVisit()) return;
    this.isClosingVisit.set(true);
    this.closeVisitError.set(null);
    this.visitApi.closeVisit(visitId).subscribe({
      next: () => {
        this.isClosingVisit.set(false);
        this.showCloseConfirmModal.set(false);
        this.visitIdToClose.set(null);
        const currentDrawerVisit = this.selectedVisitForDrawer();
        if (currentDrawerVisit?.id === visitId) this.closeVisitDrawer();
        this.loadQueue();
      },
      error: (err) => {
        this.isClosingVisit.set(false);
        this.closeVisitError.set(err.error?.detail || this.t('dashboard.queue.closeError'));
      },
    });
  }

  t(key: string): string {
    return this.i18n.t(key);
  }

  startConsultation(visitId: string): void {
    if (this.canStartConsultation()) this.router.navigate(['/clinic/consultation', visitId]);
  }

  get computedBmi(): number | null {
    if (!this.vitalsWeight || !this.vitalsHeight || this.vitalsHeight <= 0) return null;
    const heightM = this.vitalsHeight / 100;
    return parseFloat((this.vitalsWeight / (heightM * heightM)).toFixed(2));
  }

  getBmiClass(bmi?: number): string {
    if (!bmi)
      return 'bg-[var(--app-surface-muted)] text-[var(--text-secondary)] dark:bg-slate-900 dark:text-[var(--text-muted)]';
    if (bmi < 18.5)
      return 'bg-[var(--brand-warning-subtle)] text-[var(--brand-warning-text)] bg-[var(--brand-warning-subtle)] text-[var(--brand-warning-text)]';
    if (bmi < 25)
      return 'bg-[var(--brand-success-subtle)] text-[var(--brand-success-text)] bg-[var(--brand-success-subtle)] text-[var(--brand-success-text)]';
    if (bmi < 30)
      return 'bg-[var(--brand-warning-subtle)] text-[var(--brand-warning-text)] bg-[var(--brand-warning-subtle)] text-[var(--brand-warning-text)]';
    return 'bg-[var(--brand-danger-subtle)] text-red-700 dark:text-[var(--brand-danger-text)]';
  }

  isTempInvalid(): boolean {
    return (
      this.vitalsTemp !== undefined &&
      this.vitalsTemp !== null &&
      (this.vitalsTemp < 30 || this.vitalsTemp > 45)
    );
  }
  isWeightInvalid(): boolean {
    return (
      this.vitalsWeight !== undefined &&
      this.vitalsWeight !== null &&
      (this.vitalsWeight < 1 || this.vitalsWeight > 500)
    );
  }
  isHeightInvalid(): boolean {
    return (
      this.vitalsHeight !== undefined &&
      this.vitalsHeight !== null &&
      (this.vitalsHeight < 30 || this.vitalsHeight > 250)
    );
  }
  isPulseInvalid(): boolean {
    return (
      this.vitalsPulse !== undefined &&
      this.vitalsPulse !== null &&
      (this.vitalsPulse < 20 || this.vitalsPulse > 250)
    );
  }
  isSystolicInvalid(): boolean {
    return (
      this.vitalsSystolic !== undefined &&
      this.vitalsSystolic !== null &&
      (this.vitalsSystolic < 40 || this.vitalsSystolic > 250)
    );
  }
  isDiastolicInvalid(): boolean {
    return (
      this.vitalsDiastolic !== undefined &&
      this.vitalsDiastolic !== null &&
      (this.vitalsDiastolic < 30 || this.vitalsDiastolic > 150)
    );
  }
  isSpo2Invalid(): boolean {
    return (
      this.vitalsSpo2 !== undefined &&
      this.vitalsSpo2 !== null &&
      (this.vitalsSpo2 < 50 || this.vitalsSpo2 > 100)
    );
  }
  isGlycemiaInvalid(): boolean {
    return (
      this.vitalsGlycemia !== undefined &&
      this.vitalsGlycemia !== null &&
      (this.vitalsGlycemia < 0.1 || this.vitalsGlycemia > 10.0)
    );
  }
  isRespInvalid(): boolean {
    return (
      this.vitalsResp !== undefined &&
      this.vitalsResp !== null &&
      (this.vitalsResp < 5 || this.vitalsResp > 100)
    );
  }
  isPainInvalid(): boolean {
    return (
      this.vitalsPain !== undefined &&
      this.vitalsPain !== null &&
      (this.vitalsPain < 0 || this.vitalsPain > 10)
    );
  }

  isAnyVitalInvalid(): boolean {
    return (
      this.isTempInvalid() ||
      this.isWeightInvalid() ||
      this.isHeightInvalid() ||
      this.isPulseInvalid() ||
      this.isSystolicInvalid() ||
      this.isDiastolicInvalid() ||
      this.isSpo2Invalid() ||
      this.isGlycemiaInvalid() ||
      this.isRespInvalid() ||
      this.isPainInvalid()
    );
  }

  openVitalsModal(visit: Visit): void {
    if (!this.hasPermission('VISIT_VITALS_WRITE')) return;
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
      this.vitalsPain = visit.vitals.painScale;
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
      this.vitalsPain = undefined;
    }

    this.showVitalsModal.set(true);
  }

  closeVitalsModal(): void {
    if (this.isSavingVitals()) return;
    this.showVitalsModal.set(false);
    this.selectedVisitForVitals.set(null);
  }

  submitVitals(): void {
    const selectedVisit = this.selectedVisitForVitals();
    if (!this.hasPermission('VISIT_VITALS_WRITE') || !selectedVisit || this.isSavingVitals())
      return;
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
      respiratoryRate: this.vitalsResp,
      painScale: this.vitalsPain,
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
        this.vitalsError.set(
          err.error?.detail || err.error?.title || this.t('dashboard.vitals.saveError'),
        );
      },
    });
  }

  openAuditSecurityModal(): void {
    this.showAuditSecurityModal.set(true);
  }

  closeAuditSecurityModal(): void {
    this.showAuditSecurityModal.set(false);
  }

  applyVitalsAssistantProposal(proposal: AiVitalsProposal): void {
    const vitals = proposal.vitals;
    if (typeof vitals.temperature === 'number') this.vitalsTemp = vitals.temperature;
    if (typeof vitals.weight === 'number') this.vitalsWeight = vitals.weight;
    if (typeof vitals.height === 'number') this.vitalsHeight = vitals.height;
    if (typeof vitals.pulse === 'number') this.vitalsPulse = vitals.pulse;
    if (typeof vitals.systolic === 'number') this.vitalsSystolic = vitals.systolic;
    if (typeof vitals.diastolic === 'number') this.vitalsDiastolic = vitals.diastolic;
    if (typeof vitals.spo2 === 'number') this.vitalsSpo2 = vitals.spo2;
    if (typeof vitals.glycemia === 'number') this.vitalsGlycemia = vitals.glycemia;
    if (typeof vitals.respiratoryRate === 'number') this.vitalsResp = vitals.respiratoryRate;
    if (typeof vitals.painScale === 'number') this.vitalsPain = vitals.painScale;
  }

  currentVitalsForAssistant(): Partial<Record<AiVitalField, number>> {
    const values: Array<[AiVitalField, number | undefined]> = [
      ['temperature', this.vitalsTemp],
      ['weight', this.vitalsWeight],
      ['height', this.vitalsHeight],
      ['pulse', this.vitalsPulse],
      ['systolic', this.vitalsSystolic],
      ['diastolic', this.vitalsDiastolic],
      ['spo2', this.vitalsSpo2],
      ['glycemia', this.vitalsGlycemia],
      ['respiratoryRate', this.vitalsResp],
      ['painScale', this.vitalsPain],
    ];
    return Object.fromEntries(
      values.filter((entry): entry is [AiVitalField, number] => typeof entry[1] === 'number'),
    );
  }
}
