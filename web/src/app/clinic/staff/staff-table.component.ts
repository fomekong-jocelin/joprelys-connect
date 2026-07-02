import { DatePipe } from '@angular/common';
import { Component, input, output } from '@angular/core';
import { ButtonComponent } from '../../shared/ui/button.component';
import { CardComponent } from '../../shared/ui/card.component';
import { EmptyStateComponent } from '../../shared/ui/empty-state.component';
import { StatusBadgeComponent } from '../../shared/ui/status-badge.component';
import { StaffMember, StaffRole } from './staff.models';

export interface StaffTableLabels {
  readonly title: string;
  readonly loading: string;
  readonly empty: string;
  readonly member: string;
  readonly role: string;
  readonly status: string;
  readonly createdAt: string;
  readonly actions: string;
  readonly edit: string;
  readonly activate: string;
  readonly deactivate: string;
  readonly active: string;
  readonly inactive: string;
  readonly roleLabels: Record<StaffRole, string>;
}

@Component({
  selector: 'app-staff-table',
  standalone: true,
  imports: [ButtonComponent, CardComponent, DatePipe, EmptyStateComponent, StatusBadgeComponent],
  template: `
    <app-ui-card [title]="labels().title">
      @if (loading()) {
        <div class="py-10 text-center text-sm font-semibold" style="color: var(--text-secondary)">
          {{ labels().loading }}
        </div>
      } @else if (staff().length === 0) {
        <app-empty-state [message]="labels().empty" />
      } @else {
        <div class="space-y-3 md:hidden">
          @for (member of staff(); track member.id) {
            <article class="ui-card-muted space-y-4 p-4">
              <div class="flex items-start justify-between gap-3">
                <div class="min-w-0">
                  <h3 class="truncate font-extrabold" style="color: var(--text-primary)">{{ member.displayName }}</h3>
                  <p class="mt-1 break-words text-sm" style="color: var(--text-secondary)">{{ member.email }}</p>
                </div>
                <app-status-badge [active]="member.enabled" [label]="statusLabel(member)" />
              </div>
              <dl class="grid grid-cols-2 gap-3 text-sm">
                <div>
                  <dt class="ui-label">{{ labels().role }}</dt>
                  <dd class="mt-1 font-bold" style="color: var(--text-primary)">{{ roleLabel(member.role) }}</dd>
                </div>
                <div>
                  <dt class="ui-label">{{ labels().createdAt }}</dt>
                  <dd class="mt-1 font-semibold" style="color: var(--text-secondary)">
                    {{ member.createdAt | date:'dd/MM/yyyy' }}
                  </dd>
                </div>
              </dl>
              <div class="grid grid-cols-2 gap-2">
                <app-ui-button variant="secondary" (pressed)="editRequested.emit(member)">
                  {{ labels().edit }}
                </app-ui-button>
                <app-ui-button [variant]="member.enabled ? 'danger' : 'primary'" (pressed)="statusToggled.emit(member)">
                  {{ member.enabled ? labels().deactivate : labels().activate }}
                </app-ui-button>
              </div>
            </article>
          }
        </div>

        <div class="hidden overflow-x-auto md:block">
          <table class="ui-table">
            <thead>
              <tr>
                <th>{{ labels().member }}</th>
                <th>{{ labels().role }}</th>
                <th>{{ labels().status }}</th>
                <th>{{ labels().createdAt }}</th>
                <th class="text-right">{{ labels().actions }}</th>
              </tr>
            </thead>
            <tbody>
              @for (member of staff(); track member.id) {
                <tr>
                  <td>
                    <div class="font-extrabold" style="color: var(--text-primary)">{{ member.displayName }}</div>
                    <div class="mt-1 text-xs" style="color: var(--text-muted)">{{ member.email }}</div>
                  </td>
                  <td>{{ roleLabel(member.role) }}</td>
                  <td><app-status-badge [active]="member.enabled" [label]="statusLabel(member)" /></td>
                  <td>{{ member.createdAt | date:'dd/MM/yyyy' }}</td>
                  <td class="text-right">
                    <div class="inline-flex items-center gap-3">
                      <app-ui-button variant="link" (pressed)="editRequested.emit(member)">
                        {{ labels().edit }}
                      </app-ui-button>
                      <app-ui-button [variant]="member.enabled ? 'danger' : 'link'" (pressed)="statusToggled.emit(member)">
                        {{ member.enabled ? labels().deactivate : labels().activate }}
                      </app-ui-button>
                    </div>
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
export class StaffTableComponent {
  readonly staff = input.required<readonly StaffMember[]>();
  readonly labels = input.required<StaffTableLabels>();
  readonly loading = input(false);
  readonly editRequested = output<StaffMember>();
  readonly statusToggled = output<StaffMember>();

  roleLabel(role: StaffRole): string {
    return this.labels().roleLabels[role];
  }

  statusLabel(member: StaffMember): string {
    return member.enabled ? this.labels().active : this.labels().inactive;
  }
}
