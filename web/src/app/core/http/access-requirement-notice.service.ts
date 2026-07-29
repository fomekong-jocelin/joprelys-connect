import { Injectable, signal } from '@angular/core';

export interface AccessRequirementNotice {
  code: 'CONSENT_REQUIRED' | 'SCOPE_REQUIRED';
  message: string;
  requiredScope?: string;
}

@Injectable({ providedIn: 'root' })
export class AccessRequirementNoticeService {
  readonly notice = signal<AccessRequirementNotice | null>(null);

  show(notice: AccessRequirementNotice): void {
    this.notice.set(notice);
  }

  clear(): void {
    this.notice.set(null);
  }
}
