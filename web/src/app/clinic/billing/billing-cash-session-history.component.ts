import { CommonModule } from '@angular/common';
import { Component, DestroyRef, OnInit, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { finalize } from 'rxjs';
import { I18nService } from '../../core/i18n/i18n.service';
import { BillingApiService } from '../../patient/billing-api.service';
import { CashSessionHistory } from '../../patient/cash-session-history.models';
import { CashMovement } from '../../patient/patient.models';
import { IconComponent } from '../../shared/ui/icon.component';

@Component({
  selector: 'app-billing-cash-session-history',
  standalone: true,
  imports: [CommonModule, IconComponent],
  templateUrl: './billing-cash-session-history.component.html',
  styleUrl: './billing-cash-session-history.component.css',
})
export class BillingCashSessionHistoryComponent implements OnInit {
  private readonly billingApi = inject(BillingApiService);
  private readonly i18n = inject(I18nService);
  private readonly destroyRef = inject(DestroyRef);

  readonly sessions = signal<CashSessionHistory[]>([]);
  readonly loading = signal(false);
  readonly error = signal(false);
  readonly expandedSessionId = signal<string | null>(null);
  readonly highlightedSessionId = signal<string | null>(null);
  readonly loadingMovementsId = signal<string | null>(null);
  readonly downloadingSessionId = signal<string | null>(null);
  readonly movementsBySession = signal<Record<string, CashMovement[]>>({});

  ngOnInit(): void {
    this.loadSessions();
    this.billingApi.cashSessionClosed$
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((session) => {
        this.highlightedSessionId.set(session.id);
        this.expandedSessionId.set(session.id);
        this.loadSessions();
        this.loadMovements(session.id);
      });
  }

  t(key: string, fallback: string): string {
    return this.i18n.t(key, fallback);
  }

  loadSessions(): void {
    this.loading.set(true);
    this.error.set(false);
    this.billingApi.listMyCashSessions()
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: (sessions) => this.sessions.set(sessions),
        error: () => this.error.set(true),
      });
  }

  toggleDetails(session: CashSessionHistory): void {
    if (this.expandedSessionId() === session.id) {
      this.expandedSessionId.set(null);
      return;
    }
    this.expandedSessionId.set(session.id);
    this.loadMovements(session.id);
  }

  movementsFor(sessionId: string): CashMovement[] {
    return this.movementsBySession()[sessionId] ?? [];
  }

  downloadReport(session: CashSessionHistory): void {
    if (session.status !== 'CLOSED' || this.downloadingSessionId()) return;
    this.downloadingSessionId.set(session.id);
    this.billingApi.downloadCashCloseoutReport(session.id)
      .pipe(finalize(() => this.downloadingSessionId.set(null)))
      .subscribe({
        next: (blob) => {
          const url = URL.createObjectURL(blob);
          const anchor = document.createElement('a');
          anchor.href = url;
          anchor.download = `BORDEREAU_CLOTURE_${session.reportNumber}.pdf`;
          anchor.click();
          URL.revokeObjectURL(url);
        },
        error: () => this.error.set(true),
      });
  }

  private loadMovements(sessionId: string): void {
    if (this.movementsBySession()[sessionId] || this.loadingMovementsId() === sessionId) return;
    this.loadingMovementsId.set(sessionId);
    this.billingApi.getSessionMovements(sessionId)
      .pipe(finalize(() => this.loadingMovementsId.set(null)))
      .subscribe({
        next: (movements) => this.movementsBySession.update((current) => ({
          ...current,
          [sessionId]: movements,
        })),
        error: () => this.error.set(true),
      });
  }
}