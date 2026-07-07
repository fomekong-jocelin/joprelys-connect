import { Component, computed, inject, input, model, output } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { AlertComponent } from '../../shared/ui/alert.component';
import { ButtonComponent } from '../../shared/ui/button.component';
import { CardComponent } from '../../shared/ui/card.component';
import { InputComponent } from '../../shared/ui/input.component';
import { FileDragDropComponent } from '../../shared/ui/file-drag-drop.component';

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
  readonly country: string;
  readonly countryPlaceholder: string;
  readonly type: string;
  readonly responsibleName: string;
  readonly responsibleNamePlaceholder: string;
  readonly apiEnabled: string;
  readonly cancel: string;
  readonly save: string;
  readonly saving: string;
}

@Component({
  selector: 'app-organization-form',
  standalone: true,
  imports: [AlertComponent, ButtonComponent, CardComponent, InputComponent, FileDragDropComponent],
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
          <app-ui-input
            [label]="labels().country"
            [placeholder]="labels().countryPlaceholder"
            [required]="true"
            [(value)]="country"
          />
          <div class="space-y-1.5 w-full">
            <label class="ui-label">
              {{ labels().type }} <span class="text-[var(--brand-danger)]">*</span>
            </label>
            <select
              [value]="type()"
              (change)="type.set($any($event.target).value)"
              class="ui-input bg-[var(--app-surface)] text-slate-900 dark:text-slate-100"
              required
            >
              <option value="HOSPITAL">Hôpital</option>
              <option value="CLINIC">Clinique</option>
              <option value="CABINET">Cabinet médical</option>
              <option value="LABORATORY">Laboratoire</option>
              <option value="IMAGING_CENTER">Centre d'imagerie</option>
              <option value="PHARMACY">Pharmacie</option>
              <option value="HEALTH_PLATFORM">Plateforme santé</option>
              <option value="NGO">Association / ONG</option>
              <option value="INSTITUTION">Institution</option>
            </select>
          </div>
          <app-ui-input
            [label]="labels().responsibleName"
            [placeholder]="labels().responsibleNamePlaceholder"
            [required]="true"
            [(value)]="responsibleName"
          />
          <div class="flex items-center gap-3 pt-6">
            <input
              type="checkbox"
              id="apiEnabled"
              [checked]="apiEnabled()"
              (change)="apiEnabled.set($any($event.target).checked)"
              class="h-5 w-5 rounded border-[var(--app-border)] dark:border-slate-700 text-brand-primary focus:ring-brand-primary cursor-pointer"
            />
            <label for="apiEnabled" class="text-sm font-semibold text-[var(--text-secondary)] cursor-pointer">
              {{ labels().apiEnabled }}
            </label>
          </div>
          <div class="sm:col-span-2">
            <app-ui-input
              [label]="labels().address"
              [placeholder]="labels().addressPlaceholder"
              [(value)]="address"
            />
          </div>
          <div class="sm:col-span-2">
            <app-file-drag-drop
              #logoUploader
              label="Logo de l'établissement"
              [previewUrl]="logoViewUrl()"
              (fileSelected)="onLogoSelected($event, logoUploader)"
              (fileRemoved)="logoPath.set(null)"
            >
            </app-file-drag-drop>
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
  private readonly http = inject(HttpClient);

  readonly labels = input.required<OrganizationFormLabels>();
  readonly loading = input(false);
  readonly error = input<string | null>(null);

  readonly name = model('');
  readonly email = model('');
  readonly phone = model('');
  readonly address = model('');
  readonly city = model('');
  readonly country = model('Cameroun');
  readonly type = model('CLINIC');
  readonly responsibleName = model('');
  readonly apiEnabled = model(true);
  readonly logoPath = model<string | null>(null);

  readonly logoViewUrl = computed(() => this.logoPath() ? `/api/public/files/view?path=${this.logoPath()}` : null);

  readonly submitted = output<void>();
  readonly cancelled = output<void>();

  onLogoSelected(file: File, uploader: FileDragDropComponent): void {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('type', 'logo');
    this.http.post<any>('/api/files/upload', formData).subscribe({
      next: (res) => {
        this.logoPath.set(res.filePath);
        uploader.setPreviewUrl(res.viewUrl, file.name);
      },
      error: (err) => {
        console.error(err);
      }
    });
  }
}
