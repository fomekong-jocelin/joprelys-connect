import { DatePipe } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { I18nService } from '../../core/i18n/i18n.service';
import { ButtonComponent } from '../../shared/ui/button.component';
import { EmptyStateComponent } from '../../shared/ui/empty-state.component';
import { VisitApiService } from '../../visit/visit-api.service';
import { ActiveVisitScope, Visit } from '../../visit/visit.models';
import { VitalAlertsComponent } from '../../visit/vital-alerts.component';
import { orientationLabel } from '../../visit/visit-orientation.util';
import { bmiClass, careStageClass, careStageOf, hasCriticalAlert } from '../../visit/vitals-display.util';
import { RbacApiService } from '../rbac/rbac-api.service';
import { VisitDetailsDrawerComponent } from './visit-details-drawer.component';
import { VisitVitalsFormModalComponent } from './visit-vitals-form-modal.component';

/** File des visites actives : étape de prise en charge, alertes, filtre praticien et actions. */
@Component({
  selector: 'app-active-visit-queue',
  standalone: true,
  imports: [
    DatePipe,
    ButtonComponent,
    EmptyStateComponent,
    VitalAlertsComponent,
    VisitDetailsDrawerComponent,
    VisitVitalsFormModalComponent,
  ],
  templateUrl: './active-visit-queue.component.html',
})
export class ActiveVisitQueueComponent implements OnInit {
  private readonly i18n = inject(I18nService);
  private readonly visitApi = inject(VisitApiService);
  private readonly rbacApi = inject(RbacApiService);
  private readonly router = inject(Router);

  readonly scopes: readonly ActiveVisitScope[] = ['ALL', 'MINE', 'SERVICE'];
  readonly scope = signal<ActiveVisitScope>('ALL');

  readonly activeVisits = signal<Visit[]>([]);
  readonly isLoadingQueue = signal(false);
  readonly queueError = signal('');
  readonly actionError = signal('');

  readonly selectedVisitForDrawer = signal<Visit | null>(null);
  readonly selectedVisitForVitals = signal<Visit | null>(null);

  readonly visitIdToClose = signal<string | null>(null);
  readonly isClosingVisit = signal(false);
  readonly closeVisitError = signal<string | null>(null);

  readonly canEnterVitals = computed(() => this.rbacApi.hasPermission('VISIT_VITALS_WRITE'));
  readonly canStartConsultation = computed(() => this.rbacApi.hasPermission('CLINICAL_WRITE'));
  readonly canCloseVisit = computed(() => this.rbacApi.hasPermission('VISIT_MANAGE'));
  readonly currentUserId = computed(() => this.rbacApi.access()?.userId ?? null);

  readonly stageCounts = computed(() => {
    const counts = { ATTENTE_CONSTANTES: 0, PRET_MEDECIN: 0, EN_CONSULTATION: 0 };
    for (const visit of this.activeVisits()) counts[careStageOf(visit)]++;
    return counts;
  });
  readonly criticalCount = computed(
    () => this.activeVisits().filter((visit) => hasCriticalAlert(visit.vitals?.alerts)).length,
  );

  ngOnInit(): void {
    this.loadQueue();
  }

  t(key: string): string {
    return this.i18n.t(key);
  }

  setScope(scope: ActiveVisitScope): void {
    if (this.scope() === scope) return;
    this.scope.set(scope);
    this.loadQueue();
  }

  loadQueue(): void {
    this.isLoadingQueue.set(true);
    this.queueError.set('');
    this.visitApi.getActiveVisits(this.scope()).subscribe({
      next: (data) => {
        this.activeVisits.set([...data].sort((a, b) => this.arrivalTimestamp(a) - this.arrivalTimestamp(b)));
        this.isLoadingQueue.set(false);
        const drawerVisit = this.selectedVisitForDrawer();
        if (drawerVisit) this.selectedVisitForDrawer.set(data.find((v) => v.id === drawerVisit.id) ?? null);
      },
      error: (err) => {
        this.isLoadingQueue.set(false);
        this.queueError.set(err.error?.detail || this.t('dashboard.queue.loadError'));
      },
    });
  }

  queueSummaryLabel(): string {
    const counts = this.stageCounts();
    const parts = [
      `${this.activeVisits().length} ${this.t('dashboard.queue.summary.active')}`,
      `${counts.ATTENTE_CONSTANTES} ${this.t('queue.summary.waitingVitals')}`,
      `${counts.PRET_MEDECIN} ${this.t('queue.summary.ready')}`,
      `${counts.EN_CONSULTATION} ${this.t('queue.summary.inConsultation')}`,
    ];
    if (this.criticalCount() > 0) parts.push(`${this.criticalCount()} ${this.t('queue.summary.critical')}`);
    const longestWait = this.longestQueueWaitMinutes();
    if (longestWait > 0) {
      parts.push(`${this.t('dashboard.queue.summary.maxWait')} ${this.formatWaitDuration(longestWait)}`);
    }
    return parts.join(' · ');
  }

  longestQueueWaitMinutes(): number {
    const now = Date.now();
    return this.activeVisits().reduce((longest, visit) => {
      const timestamp = this.arrivalTimestamp(visit);
      if (!Number.isFinite(timestamp)) return longest;
      return Math.max(longest, Math.max(0, Math.floor((now - timestamp) / 60_000)));
    }, 0);
  }

  arrivalOf(visit: Visit): string {
    return visit.arrivalAt || visit.createdAt;
  }

  stageOf(visit: Visit) {
    return careStageOf(visit);
  }

  stageClass(visit: Visit): string {
    return careStageClass(careStageOf(visit));
  }

  orientation(visit: Visit): string {
    return orientationLabel(visit.orientation, this.i18n);
  }

  isCritical(visit: Visit): boolean {
    return hasCriticalAlert(visit.vitals?.alerts);
  }

  getBmiClass(bmi?: number): string {
    return bmiClass(bmi);
  }

  openVisitDrawer(visit: Visit): void {
    this.actionError.set('');
    this.selectedVisitForDrawer.set(visit);
  }

  closeVisitDrawer(): void {
    this.selectedVisitForDrawer.set(null);
  }

  openVitalsModal(visit: Visit): void {
    if (this.canEnterVitals()) this.selectedVisitForVitals.set(visit);
  }

  closeVitalsModal(): void {
    this.selectedVisitForVitals.set(null);
  }

  onVitalsSaved(): void {
    this.selectedVisitForVitals.set(null);
    this.loadQueue();
  }

  /** Prend le patient en charge côté serveur, puis ouvre la consultation. */
  startConsultation(visit: Visit, takeOver = false): void {
    if (!this.canStartConsultation()) return;
    this.actionError.set('');
    this.visitApi.takeCharge(visit.id, takeOver).subscribe({
      next: () => void this.router.navigate(['/clinic/consultation', visit.id]),
      error: (err) => {
        this.actionError.set(err.error?.detail || this.t('queue.error.takeCharge'));
        this.loadQueue();
      },
    });
  }

  releaseConsultation(visit: Visit): void {
    this.actionError.set('');
    this.visitApi.releaseCharge(visit.id).subscribe({
      next: () => this.loadQueue(),
      error: (err) => this.actionError.set(err.error?.detail || this.t('queue.error.release')),
    });
  }

  openCloseConfirmModal(visitId: string): void {
    if (!this.canCloseVisit()) return;
    this.visitIdToClose.set(visitId);
    this.closeVisitError.set(null);
  }

  cancelCloseConfirm(): void {
    if (!this.isClosingVisit()) this.visitIdToClose.set(null);
  }

  confirmCloseVisit(): void {
    const visitId = this.visitIdToClose();
    if (!this.canCloseVisit() || !visitId || this.isClosingVisit()) return;
    this.isClosingVisit.set(true);
    this.closeVisitError.set(null);
    this.visitApi.closeVisit(visitId).subscribe({
      next: () => {
        this.isClosingVisit.set(false);
        this.visitIdToClose.set(null);
        if (this.selectedVisitForDrawer()?.id === visitId) this.closeVisitDrawer();
        this.loadQueue();
      },
      error: (err) => {
        this.isClosingVisit.set(false);
        this.closeVisitError.set(err.error?.detail || this.t('dashboard.queue.closeError'));
      },
    });
  }

  private arrivalTimestamp(visit: Visit): number {
    const timestamp = Date.parse(visit.arrivalAt || visit.createdAt);
    return Number.isNaN(timestamp) ? Number.POSITIVE_INFINITY : timestamp;
  }

  private formatWaitDuration(minutes: number): string {
    if (minutes < 60) return `${minutes} min`;
    const hours = Math.floor(minutes / 60);
    const remainingMinutes = minutes % 60;
    return remainingMinutes > 0 ? `${hours} h ${remainingMinutes} min` : `${hours} h`;
  }
}
