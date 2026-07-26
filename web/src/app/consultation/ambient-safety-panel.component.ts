import { Component, Input } from '@angular/core';
import { AmbientSpeakerReviewComponent } from './ambient-speaker-review.component';
import { DoctorVoiceCalibrationComponent } from './doctor-voice-calibration.component';

@Component({
  selector: 'app-ambient-safety-panel',
  standalone: true,
  imports: [DoctorVoiceCalibrationComponent, AmbientSpeakerReviewComponent],
  template: `
    <div class="space-y-3">
      <app-doctor-voice-calibration [visitId]="visitId" />
      <section class="rounded-[6px] border border-[var(--app-border)] bg-[var(--app-surface)] p-3 shadow-sm">
        <app-ambient-speaker-review [visitId]="visitId" />
      </section>
    </div>
  `,
})
export class AmbientSafetyPanelComponent {
  @Input({ required: true }) visitId = '';
}
