import { CommonModule } from '@angular/common';
import { Component, inject, input, output } from '@angular/core';
import { I18nService } from '../core/i18n/i18n.service';
import { Vitals } from '../visit/visit.models';
import { VitalAlertsComponent } from '../visit/vital-alerts.component';
import { VitalsHistoryComponent } from '../visit/vitals-history.component';
import { ConsultationUiLabelsService } from './consultation-ui-labels.service';
@Component({
  selector: 'app-consultation-vitals-panel',
  imports: [CommonModule, VitalAlertsComponent, VitalsHistoryComponent],
  templateUrl: './consultation-vitals-panel.component.html',
})
export class ConsultationVitalsPanelComponent {
  readonly vitals = input<Vitals | null>(null);
  readonly pendingVitalsProposal = input<Vitals | null>(null);
  readonly visitId = input('');
  readonly confirmed = output<void>();
  readonly dismissed = output<void>();
  readonly i18n = inject(I18nService);
  readonly uiLabels = inject(ConsultationUiLabelsService);
  vitalsProposalEntries(): Array<[string, number]> {
    return Object.entries(this.pendingVitalsProposal() ?? {}).filter(
      (entry): entry is [string, number] => typeof entry[1] === 'number');
  }
}
