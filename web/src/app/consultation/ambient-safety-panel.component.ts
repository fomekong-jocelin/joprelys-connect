import { Component, Input, signal } from '@angular/core';
import { AmbientSpeakerReviewComponent } from './ambient-speaker-review.component';
import { DoctorVoiceCalibrationComponent } from './doctor-voice-calibration.component';

@Component({
  selector: 'app-ambient-safety-panel',
  standalone: true,
  imports: [DoctorVoiceCalibrationComponent, AmbientSpeakerReviewComponent],
  template: `
    <div class="space-y-2">
      <app-ambient-speaker-review
        [visitId]="visitId"
        (ambiguityChange)="pendingAmbiguities.set($event)"
      />
      @if (pendingAmbiguities() > 0) {
        <app-doctor-voice-calibration [visitId]="visitId" [compact]="true" />
      }
    </div>
  `,
})
export class AmbientSafetyPanelComponent {
  @Input({ required: true }) visitId = '';
  readonly pendingAmbiguities = signal(0);
}
