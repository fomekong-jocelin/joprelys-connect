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
        <div class="space-y-3 md:hidden">
          @for (org of organizations(); track org.id) {
            <article class="ui-card-muted p-4">
              <div class="flex items-start justify-between gap-3">
                <div class="min-w-0">
                  <h3 class="truncate font-extrabold" style="color: var(--text-primary)">{{ org.name }}</h3>
                  <p class="mt-1 text-sm" style="color: var(--text-secondary)">{{ org.city }}</p>
                </div>
                <app-status-badge
                  [active]="org.status === 'ACTIVE'"
                  [label]="org.status === 'ACTIVE' ? labels().active : labels().inactive"
                />
              </div>

              <dl class="mt-4 space-y-3 text-sm">
                <div>
                  <dt class="ui-label">{{ labels().contact }}</dt>
                  <dd class="mt-1 break-words" style="color: var(--text-secondary)">{{ org.email }}</dd>
                  @if (org.phone) {
                    <dd class="mt-1" style="color: var(--text-muted)">{{ org.phone }}</dd>
                  }
                </div>
                @if (org.address) {
                  <div>
                    <dt class="ui-label">{{ labels().address }}</dt>
                    <dd class="mt-1" style="color: var(--text-secondary)">{{ org.address }}</dd>
                  </div>
                }
              </dl>

              <div class="mt-4 flex flex-col gap-2">
                <app-ui-button
                  class="w-full"
                  [variant]="org.status === 'ACTIVE' ? 'secondary' : 'primary'"
                  (pressed)="statusToggled.emit(org)"
                >
                  {{ org.status === 'ACTIVE' ? labels().deactivate : labels().activate }}
                </app-ui-button>
                <app-ui-button
                  class="w-full"
                  variant="link"
                  (pressed)="adminRequested.emit(org)"
                >
                  {{ labels().assignAdmin }}
                </app-ui-button>
              </div>
            </article>
          }
        </div>

        <div class="hidden overflow-x-auto md:block">
          <table class="ui-table">
            <thead>
              <tr>
                <th>{{ labels().clinic }}</th>
                <th>{{ labels().city }}</th>
                <th>{{ labels().contact }}</th>
                <th>{{ labels().status }}</th>
                <th class="text-right">{{ labels().actions }}</th>
              </tr>
            </thead>
            <tbody>
              @for (org of organizations(); track org.id) {
                <tr>
                  <td>
                    <div class="font-extrabold" style="color: var(--text-primary)">{{ org.name }}</div>
                    @if (org.address) {
                      <div class="mt-1 text-xs" style="color: var(--text-muted)">{{ org.address }}</div>
                    }
                  </td>
                  <td>{{ org.city }}</td>
                  <td>
                    <div>{{ org.email }}</div>
                    @if (org.phone) {
                      <div class="mt-1 text-xs" style="color: var(--text-muted)">{{ org.phone }}</div>
                    }
                  </td>
                  <td>
                    <app-status-badge
                      [active]="org.status === 'ACTIVE'"
                      [label]="org.status === 'ACTIVE' ? labels().active : labels().inactive"
                    />
                  </td>
                  <td class="text-right">
                    <div class="inline-flex flex-col items-end gap-1">
                      <app-ui-button
                        [variant]="org.status === 'ACTIVE' ? 'danger' : 'link'"
                        (pressed)="statusToggled.emit(org)"
                      >
                        {{ org.status === 'ACTIVE' ? labels().deactivate : labels().activate }}
                      </app-ui-button>
                      <app-ui-button
                        variant="link"
                        (pressed)="adminRequested.emit(org)"
                      >
                        {{ labels().assignAdmin }}
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
export class OrganizationTableComponent {
  readonly organizations = input.required<readonly Organization[]>();
  readonly labels = input.required<OrganizationTableLabels>();
  readonly loading = input(false);
  readonly statusToggled = output<Organization>();
  readonly adminRequested = output<Organization>();
}
