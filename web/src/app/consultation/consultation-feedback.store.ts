import { Injectable, signal } from '@angular/core';

@Injectable()
export class ConsultationFeedbackStore {
  readonly isLoading = signal(false);
  readonly isSaving = signal(false);
  readonly isClosing = signal(false);
  readonly successMessage = signal('');
  readonly errorMessage = signal('');
}
