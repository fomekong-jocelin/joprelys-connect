import { Component, input, output } from '@angular/core';
import { ButtonComponent } from '../../shared/ui/button.component';
import { CardComponent } from '../../shared/ui/card.component';
import { EmptyStateComponent } from '../../shared/ui/empty-state.component';
import { StatusBadgeComponent } from '../../shared/ui/status-badge.component';
import { Organization } from './organizations.models';

export interface OrganizationTableLabels {
  readonly title: string;
  readonly loading: string;
  readonly empty: string;
  readonly clinic: string;
  readonly city: string;
  readonly contact: string;
  readonly address: string;
  readonly status: string;
  readonly actions: string;
  readonly active: string;
  readonly inactive: string;
  readonly activate: string;
  readonly deactivate: string;
  readonly assignAdmin: string;
}

@Component({
  selector: 'app-organization-table',
  standalone: true,
  imports: [ButtonComponent, CardComponent, EmptyStateComponent, StatusBadgeComponent],
  template: `
    <app-ui-card [title]="labels().title">
      @if (loading()) {
        <div class="py-10 text-center text-sm font-semibold" style="color: var(--text-secondary)">
          {{ labels().loading }}
        </div>
      } @else if (organizations().length === 0) {
        <app-empty-state [message]="labels().empty" />
      } @else {
        <!-- Vue Mobile -->
        <div class="space-y-3 md:hidden">
          @for (org of organizations(); track org.id) {
            <article class="ui-card-muted p-4 border border-[var(--app-border)]/80 rounded-lg">
              <div class="flex items-start justify-between gap-3">
                <div class="min-w-0">
                  <h3 class="font-extrabold text-[var(--text-primary)] hover:text-brand-cyan cursor-pointer transition-colors" (click)="detailRequested.emit(org)">{{ org.name }}</h3>
                  <p class="mt-1 text-sm text-[var(--text-muted)]">{{ org.city }}</p>
                </div>
                <app-status-badge
                  [active]="org.status === 'ACTIVE'"
                  [label]="org.status === 'ACTIVE' ? labels().active : labels().inactive"
                />
              </div>

              <dl class="mt-4 space-y-3 text-xs">
                <div>
                  <dt class="font-semibold text-[var(--text-muted)] uppercase tracking-wider text-[10px]">{{ labels().contact }}</dt>
                  <dd class="mt-1 text-[var(--text-secondary)] break-all">{{ org.email }}</dd>
                  @if (org.phone) {
                    <dd class="mt-1 text-[var(--text-muted)]">{{ org.phone }}</dd>
                  }
                </div>
                @if (org.address) {
                  <div>
                    <dt class="font-semibold text-[var(--text-muted)] uppercase tracking-wider text-[10px]">{{ labels().address }}</dt>
                    <dd class="mt-1 text-[var(--text-secondary)]">{{ org.address }}</dd>
                  </div>
                }
                <div>
                  <dt class="font-semibold text-[var(--text-muted)] uppercase tracking-wider text-[10px]">Administrateur</dt>
                  <dd class="mt-1 text-[var(--text-secondary)]">
                    @if (org.adminEmail) {
                      <span class="font-bold text-[var(--text-primary)]">{{ org.adminDisplayName }}</span>
                      <span class="text-[var(--text-muted)] font-mono text-[11px] block mt-0.5">{{ org.adminEmail }}</span>
                    } @else {
                      <span class="text-[var(--text-muted)] italic">Aucun administrateur affecté</span>
                    }
                  </dd>
                </div>
              </dl>

              <div class="mt-4 flex flex-col gap-2">
                <app-ui-button
                  class="w-full justify-center"
                  variant="secondary"
                  (pressed)="detailRequested.emit(org)"
                >
                  Gérer
                </app-ui-button>
                <app-ui-button
                  class="w-full justify-center"
                  [variant]="org.status === 'ACTIVE' ? 'secondary' : 'primary'"
                  (pressed)="statusToggled.emit(org)"
                >
                  {{ org.status === 'ACTIVE' ? labels().deactivate : labels().activate }}
                </app-ui-button>
              </div>
            </article>
          }
        </div>

        <!-- Vue Desktop -->
        <div class="hidden overflow-x-auto md:block">
          <table class="ui-table w-full">
            <thead>
              <tr class="border-b border-[var(--app-border)]/80">
                <th class="py-3 px-4 text-left font-bold text-xs uppercase tracking-wider text-[var(--text-muted)]">{{ labels().clinic }}</th>
                <th class="py-3 px-4 text-left font-bold text-xs uppercase tracking-wider text-[var(--text-muted)]">{{ labels().city }}</th>
                <th class="py-3 px-4 text-left font-bold text-xs uppercase tracking-wider text-[var(--text-muted)]">{{ labels().contact }}</th>
                <th class="py-3 px-4 text-left font-bold text-xs uppercase tracking-wider text-[var(--text-muted)]">Administrateur</th>
                <th class="py-3 px-4 text-left font-bold text-xs uppercase tracking-wider text-[var(--text-muted)]">{{ labels().status }}</th>
                <th class="py-3 px-4 text-right font-bold text-xs uppercase tracking-wider text-[var(--text-muted)] whitespace-nowrap">{{ labels().actions }}</th>
              </tr>
            </thead>
            <tbody>
              @for (org of organizations(); track org.id) {
                <tr class="border-b border-slate-50 dark:border-slate-850 hover:bg-[var(--app-surface-muted)] dark:hover:bg-slate-900/30 transition-colors">
                  <td class="py-4 px-4 align-middle">
                    <div
                      class="font-extrabold text-[var(--text-primary)] hover:text-brand-cyan cursor-pointer transition-colors whitespace-nowrap"
                      (click)="detailRequested.emit(org)"
                    >
                      {{ org.name }}
                    </div>
                    @if (org.address) {
                      <div class="mt-0.5 text-xs text-[var(--text-muted)] max-w-[220px] truncate" [title]="org.address">{{ org.address }}</div>
                    }
                  </td>
                  <td class="py-4 px-4 align-middle text-[var(--text-secondary)] whitespace-nowrap">{{ org.city }}</td>
                  <td class="py-4 px-4 align-middle text-[var(--text-secondary)]">
                    <div class="text-sm font-semibold whitespace-nowrap">{{ org.email }}</div>
                    @if (org.phone) {
                      <div class="mt-0.5 text-xs text-[var(--text-muted)] whitespace-nowrap">{{ org.phone }}</div>
                    }
                  </td>
                  <td class="py-4 px-4 align-middle">
                    @if (org.adminEmail) {
                      <div class="text-sm font-bold text-slate-850 dark:text-[var(--text-primary)] whitespace-nowrap">{{ org.adminDisplayName }}</div>
                      <div class="text-xs text-[var(--text-muted)] font-mono mt-0.5 whitespace-nowrap">{{ org.adminEmail }}</div>
                    } @else {
                      <span class="inline-flex items-center px-2 py-0.5 rounded text-[10px] font-semibold bg-[var(--brand-danger-subtle)] text-red-650 dark:text-red-400 whitespace-nowrap">
                        Aucun admin
                      </span>
                    }
                  </td>
                  <td class="py-4 px-4 align-middle whitespace-nowrap">
                    <app-status-badge
                      [active]="org.status === 'ACTIVE'"
                      [label]="org.status === 'ACTIVE' ? labels().active : labels().inactive"
                    />
                  </td>
                  <td class="py-4 px-4 align-middle text-right whitespace-nowrap">
                    <app-ui-button
                      variant="secondary"
                      size="sm"
                      (pressed)="detailRequested.emit(org)"
                    >
                      Gérer
                    </app-ui-button>
                  </td>
                </tr>
              }
            </tbody>
          </table>
        </div>
      }
    </app-ui-card>
  `,
})
export class OrganizationTableComponent {
  readonly organizations = input.required<readonly Organization[]>();
  readonly labels = input.required<OrganizationTableLabels>();
  readonly loading = input(false);
  readonly statusToggled = output<Organization>();
  readonly detailRequested = output<Organization>();
}
