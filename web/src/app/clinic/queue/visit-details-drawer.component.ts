import { DatePipe } from '@angular/common';
import { Component, computed, inject, input, output } from '@angular/core';
import { I18nService } from '../../core/i18n/i18n.service';
import { ButtonComponent } from '../../shared/ui/button.component';
import { Visit } from '../../visit/visit.models';
import { VitalAlertsComponent } from '../../visit/vital-alerts.component';
import { VitalsHistoryComponent } from '../../visit/vitals-history.component';
import { bmiClass, careStageClass, careStageOf } from '../../visit/vitals-display.util';
import { orientationLabel } from '../../visit/visit-orientation.util';

/** Détail d'une visite active : constantes, étape de prise en charge et actions du praticien. */
@Component({
  selector: 'app-visit-details-drawer',
  standalone: true,
  imports: [DatePipe, ButtonComponent, VitalAlertsComponent, VitalsHistoryComponent],
  templateUrl: './visit-details-drawer.component.html',
  styleUrl: './visit-details-drawer.component.css',
})
export class VisitDetailsDrawerComponent {
  private readonly i18n = inject(I18nService);

  readonly visit = input.required<Visit>();
  readonly currentUserId = input<string | null>(null);
  readonly canEnterVitals = input(false);
  readonly canStartConsultation = input(false);
  readonly canCloseVisit = input(false);

  readonly closed = output<void>();
  readonly enterVitals = output<void>();
  /** `true` lorsqu'il s'agit de reprendre un patient pris en charge par un confrère. */
  readonly startConsultation = output<boolean>();
  readonly release = output<void>();
  readonly closeVisit = output<void>();

  readonly stage = computed(() => careStageOf(this.visit()));
  readonly heldByMe = computed(() =>
    this.stage() === 'EN_CONSULTATION' && this.visit().consultingPractitionerId === this.currentUserId(),
  );
  readonly heldByColleague = computed(() =>
    this.stage() === 'EN_CONSULTATION' && !this.heldByMe() && Boolean(this.visit().consultingPractitionerId),
  );

  t(key: string): string {
    return this.i18n.t(key);
  }

  getBmiClass(bmi?: number): string {
    return bmiClass(bmi);
  }

  stageClass(): string {
    return careStageClass(this.stage());
  }

  orientation(): string {
    return orientationLabel(this.visit().orientation, this.i18n);
  }

  startLabel(): string {
    if (this.heldByMe()) return this.t('queue.action.resumeConsultation');
    if (this.heldByColleague()) return this.t('queue.action.takeOver');
    return this.t('patient.detail.startConsultation');
  }

  requestStart(): void {
    if (this.heldByColleague()) {
      const confirmed = confirm(
        `${this.t('queue.takeOver.confirm')} ${this.visit().consultingPractitionerName ?? ''}`.trim(),
      );
      if (!confirmed) return;
    }
    this.startConsultation.emit(this.heldByColleague());
  }
}
