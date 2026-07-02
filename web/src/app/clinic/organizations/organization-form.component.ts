import { Component, input, model, output } from '@angular/core';
import { AlertComponent } from '../../shared/ui/alert.component';
import { ButtonComponent } from '../../shared/ui/button.component';
import { CardComponent } from '../../shared/ui/card.component';
import { InputComponent } from '../../shared/ui/input.component';

export interface OrganizationFormLabels {
  readonly title: string;
  readonly name: string;
  readonly namePlaceholder: string;
  readonly email: string;
  readonly emailPlaceholder: string;
  readonly phone: string;
  readonly phonePlaceholder: string;
  readonly city: string;
  readonly cityPlaceholder: string;
  readonly address: string;
  readonly addressPlaceholder: string;
  readonly cancel: string;
  readonly save: string;
  readonly saving: string;
}

@Component({
  selector: 'app-organization-form',
  standalone: true,
  imports: [AlertComponent, ButtonComponent, CardComponent, InputComponent],
  template: `
    <app-ui-card [title]="labels().title" class="mb-8">
      <form class="space-y-5" (submit)="$event.preventDefault(); submitted.emit()">
        @if (error()) {
          <app-ui-alert tone="error">{{ error() }}</app-ui-alert>
        }

        <div class="grid grid-cols-1 gap-4 sm:grid-cols-2">
          <app-ui-input
            [label]="labels().name"
            [placeholder]="labels().namePlaceholder"
            [required]="true"
            [(value)]="name"
          />
          <app-ui-input
            type="email"
            [label]="labels().email"
            [placeholder]="labels().emailPlaceholder"
            [required]="true"
            [(value)]="email"
          />
          <app-ui-input
            [label]="labels().phone"
            [placeholder]="labels().phonePlaceholder"
            [(value)]="phone"
          />
          <app-ui-input
            [label]="labels().city"
            [placeholder]="labels().cityPlaceholder"
            [required]="true"
            [(value)]="city"
          />
          <div class="sm:col-span-2">
            <app-ui-input
              [label]="labels().address"
              [placeholder]="labels().addressPlaceholder"
              [(value)]="address"
            />
          </div>
        </div>

        <div class="flex justify-end gap-3">
          <app-ui-button variant="secondary" (pressed)="cancelled.emit()">
            {{ labels().cancel }}
          </app-ui-button>
          <app-ui-button type="submit" [disabled]="loading()">
            {{ loading() ? labels().saving : labels().save }}
          </app-ui-button>
        </div>
      </form>
    </app-ui-card>
  `,
})
export class OrganizationFormComponent {
  readonly labels = input.required<OrganizationFormLabels>();
  readonly loading = input(false);
  readonly error = input<string | null>(null);

  readonly name = model('');
  readonly email = model('');
  readonly phone = model('');
  readonly address = model('');
  readonly city = model('');

  readonly submitted = output<void>();
  readonly cancelled = output<void>();
}
