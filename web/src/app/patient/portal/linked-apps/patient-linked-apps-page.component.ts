import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { I18nService } from '../../../core/i18n/i18n.service';
import { interpolate } from '../../../core/i18n/interpolate';
import { AppShellComponent } from '../../../shared/layout/app-shell.component';
import { DialogService } from '../../../shared/ui/dialog.service';
import { EmptyStateComponent } from '../../../shared/ui/empty-state.component';
import { LoadErrorComponent } from '../../../shared/ui/load-error.component';
import { PageHeaderComponent } from '../../../shared/ui/page-header.component';
import { ToastService } from '../../../shared/ui/toast.service';
import { linkedAppIdentity } from './linked-app-catalog';
import { LinkedAppCardComponent } from './linked-app-card.component';
import { PatientLinkedAppsApiService } from './linked-apps-api.service';
import { LinkedApp } from './linked-apps.models';

type LoadState = 'loading' | 'error' | 'ready';

/** Portail patient : applications de l'écosystème reliées au dossier, et révocation (S7). */
@Component({
  selector: 'app-patient-linked-apps-page',
  imports: [
    AppShellComponent,
    PageHeaderComponent,
    LoadErrorComponent,
    EmptyStateComponent,
    LinkedAppCardComponent,
    RouterLink,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <app-shell>
      <app-page-header
        [title]="i18n.t('patient.linkedApps.title')"
        [subtitle]="i18n.t('patient.linkedApps.subtitle')"
        backLink="/patient/privacy"
        [backLabel]="i18n.t('patient.linkedApps.back')"
      />

      <div class="app-container max-w-3xl pb-10">
        @switch (state()) {
          @case ('loading') {
            <div class="space-y-4" aria-busy="true" [attr.aria-label]="i18n.t('patient.linkedApps.loading')">
              @for (row of skeletonRows; track row) {
                <div class="ui-card h-44 animate-pulse p-5" aria-hidden="true">
                  <div class="flex items-center gap-3">
                    <div class="h-12 w-12" style="border-radius: var(--radius-brand-sm); background: var(--app-border)"></div>
                    <div class="h-4 w-40 rounded" style="background: var(--app-border)"></div>
                  </div>
                </div>
              }
            </div>
          }
          @case ('error') {
            <app-ui-load-error
              [message]="i18n.t('patient.linkedApps.loadError')"
              testId="linked-apps-load-error"
              (retry)="load()"
            />
          }
          @case ('ready') {
            @if (apps().length === 0) {
              <app-empty-state [message]="i18n.t('patient.linkedApps.empty')" />
              <p class="mt-3 text-center text-sm" style="color: var(--text-secondary)">
                {{ i18n.t('patient.linkedApps.emptyHint') }}
              </p>
            } @else {
              <ul class="space-y-4" role="list" [attr.aria-label]="i18n.t('patient.linkedApps.listLabel')">
                @for (app of apps(); track app.id) {
                  <li>
                    <app-linked-app-card [app]="app" [revoking]="revokingId() === app.id" (revoke)="confirmRevoke($event)" />
                  </li>
                }
              </ul>
            }
            <p class="mt-6 text-sm" style="color: var(--text-secondary)">
              {{ i18n.t('patient.linkedApps.auditHint') }}
              <a routerLink="/patient/audit" class="ui-link">{{ i18n.t('patient.linkedApps.auditLink') }}</a>
            </p>
          }
        }
      </div>
    </app-shell>
  `,
})
export class PatientLinkedAppsPageComponent implements OnInit {
  readonly i18n = inject(I18nService);
  private readonly api = inject(PatientLinkedAppsApiService);
  private readonly dialog = inject(DialogService);
  private readonly toast = inject(ToastService);

  readonly skeletonRows = [1, 2];
  readonly state = signal<LoadState>('loading');
  readonly apps = signal<LinkedApp[]>([]);
  readonly revokingId = signal<string | null>(null);

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.state.set('loading');
    this.api.list().subscribe({
      next: (apps) => {
        this.apps.set(apps);
        this.state.set('ready');
      },
      error: () => this.state.set('error'),
    });
  }

  async confirmRevoke(app: LinkedApp): Promise<void> {
    if (this.revokingId()) return;
    const params = { app: this.nameOf(app) };
    const confirmed = await this.dialog.confirm({
      title: interpolate(this.i18n.t('patient.linkedApps.confirmTitle'), params),
      message: interpolate(this.i18n.t('patient.linkedApps.confirmMessage'), params),
      confirmLabel: this.i18n.t('patient.linkedApps.confirmAction'),
      cancelLabel: this.i18n.t('common.cancel'),
      tone: 'danger',
    });
    if (confirmed) this.revoke(app, params);
  }

  private revoke(app: LinkedApp, params: { app: string }): void {
    this.revokingId.set(app.id);
    this.api.revoke(app.id).subscribe({
      next: () => {
        this.apps.update((apps) => apps.filter((candidate) => candidate.id !== app.id));
        this.revokingId.set(null);
        this.toast.success(interpolate(this.i18n.t('patient.linkedApps.revoked'), params));
      },
      error: () => {
        this.revokingId.set(null);
        this.toast.error(this.i18n.t('patient.linkedApps.revokeError'));
      },
    });
  }

  private nameOf(app: LinkedApp): string {
    return linkedAppIdentity(app.application).name ?? this.i18n.t('patient.linkedApps.unknownApp');
  }
}
