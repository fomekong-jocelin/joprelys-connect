import { Component, inject, input, model, output } from '@angular/core';
import { AlertComponent } from '../shared/ui/alert.component';
import { ButtonComponent } from '../shared/ui/button.component';
import { CardComponent } from '../shared/ui/card.component';
import { InputComponent } from '../shared/ui/input.component';
import { I18nService } from '../core/i18n/i18n.service';

export interface PatientFormLabels {
  readonly title: string;
  readonly fullName: string;
  readonly fullNamePlaceholder: string;
  readonly gender: string;
  readonly genderPlaceholder: string;
  readonly genderMale: string;
  readonly genderFemale: string;
  readonly birthDate: string;
  readonly phone: string;
  readonly phonePlaceholder: string;
  readonly city: string;
  readonly cityPlaceholder: string;
  readonly district: string;
  readonly districtPlaceholder: string;
  readonly address: string;
  readonly addressPlaceholder: string;
  readonly emergencyContactName: string;
  readonly emergencyContactNamePlaceholder: string;
  readonly emergencyContactPhone: string;
  readonly emergencyContactPhonePlaceholder: string;
  readonly emergencyContact: string;
  readonly allergies: string;
  readonly allergiesPlaceholder: string;
  readonly medicalHistory: string;
  readonly medicalHistoryPlaceholder: string;
  readonly cancel: string;
  readonly save: string;
  readonly saving: string;
}

@Component({
  selector: 'app-patient-form',
  standalone: true,
  imports: [AlertComponent, ButtonComponent, CardComponent, InputComponent],
  template: `
    <app-ui-card [title]="labels().title" class="mb-8">
      <form class="space-y-5" (submit)="$event.preventDefault(); submitted.emit()">
        @if (error()) {
          <app-ui-alert tone="error">{{ error() }}</app-ui-alert>
        }

        <!-- Section 1 : Identité administrative -->
        <div>
          <h3 class="mb-3 font-bold border-b pb-1 text-sm uppercase tracking-wider" style="color: var(--text-muted)">
            {{ t('patient.form.section.identity') }}
          </h3>
          <div class="grid grid-cols-1 gap-4 sm:grid-cols-2">
            <app-ui-input
              [label]="labels().fullName"
              [placeholder]="labels().fullNamePlaceholder"
              [required]="true"
              [(value)]="fullName"
            />
            
            <div class="flex flex-col">
              <label class="ui-label mb-1.5 font-bold">{{ labels().gender }} <span class="text-rose-500">*</span></label>
              <select
                class="ui-select w-full"
                [value]="gender()"
                (change)="onGenderChange($event)"
              >
                <option value="" disabled>{{ labels().genderPlaceholder }}</option>
                <option value="MASCULIN">{{ labels().genderMale }}</option>
                <option value="FEMININ">{{ labels().genderFemale }}</option>
              </select>
            </div>

            <!-- Groupe Sanguin -->
            <div class="flex flex-col">
              <label class="ui-label mb-1.5 font-bold">{{ t('patient.form.bloodGroup') }}</label>
              <select
                class="ui-select w-full"
                [value]="bloodGroup()"
                (change)="onBloodGroupChange($event)"
              >
                <option value="">{{ t('patient.form.bloodGroupPlaceholder') }}</option>
                <option value="O+">O+</option>
                <option value="O-">O-</option>
                <option value="A+">A+</option>
                <option value="A-">A-</option>
                <option value="B+">B+</option>
                <option value="B-">B-</option>
                <option value="AB+">AB+</option>
                <option value="AB-">AB-</option>
              </select>
            </div>

            <app-ui-input
              type="date"
              [label]="labels().birthDate"
              [required]="true"
              [(value)]="birthDate"
            />

            <app-ui-input
              [label]="labels().phone"
              [placeholder]="labels().phonePlaceholder"
              [required]="true"
              [(value)]="phone"
            />

            <!-- Adresse Email -->
            <app-ui-input
              type="email"
              [label]="t('patient.form.email')"
              [placeholder]="t('patient.form.emailPlaceholder')"
              [(value)]="email"
            />

            <app-ui-input
              [label]="labels().city"
              [placeholder]="labels().cityPlaceholder"
              [required]="true"
              [(value)]="city"
            />

            <app-ui-input
              [label]="labels().district"
              [placeholder]="labels().districtPlaceholder"
              [(value)]="district"
            />

            <div class="sm:col-span-2">
              <app-ui-input
                [label]="labels().address"
                [placeholder]="labels().addressPlaceholder"
                [(value)]="address"
              />
            </div>
          </div>
        </div>

        <!-- Section 2 : Contact d'urgence -->
        <div class="pt-4 border-t" style="border-color: var(--border-color)">
          <h3 class="mb-3 font-bold text-sm uppercase tracking-wider" style="color: var(--text-muted)">
            {{ labels().emergencyContact }}
          </h3>
          <div class="grid grid-cols-1 gap-4 sm:grid-cols-2">
            <app-ui-input
              [label]="labels().emergencyContactName"
              [placeholder]="labels().emergencyContactNamePlaceholder"
              [(value)]="emergencyContactName"
            />
            <app-ui-input
              [label]="labels().emergencyContactPhone"
              [placeholder]="labels().emergencyContactPhonePlaceholder"
              [(value)]="emergencyContactPhone"
            />
          </div>
        </div>

        <!-- Section 3 : Informations Médicales -->
        <div class="pt-4 border-t" style="border-color: var(--border-color)">
          <h3 class="mb-3 font-bold text-sm uppercase tracking-wider" style="color: var(--text-muted)">
            {{ t('patient.form.section.medicalHistory') }}
          </h3>
          <div class="grid grid-cols-1 gap-4 sm:grid-cols-2">
            <div class="flex flex-col">
              <label class="ui-label mb-1.5 font-bold">{{ labels().allergies }}</label>
              <textarea
                class="ui-textarea w-full h-[100px] resize-none"
                [placeholder]="labels().allergiesPlaceholder"
                [value]="allergies()"
                (input)="allergies.set($any($event.target).value)"
              ></textarea>
            </div>
            <div class="flex flex-col">
              <label class="ui-label mb-1.5 font-bold">{{ labels().medicalHistory }}</label>
              <textarea
                class="ui-textarea w-full h-[100px] resize-none"
                [placeholder]="labels().medicalHistoryPlaceholder"
                [value]="medicalHistory()"
                (input)="medicalHistory.set($any($event.target).value)"
              ></textarea>
            </div>
          </div>
        </div>

        <div class="flex justify-end gap-3 pt-4 border-t" style="border-color: var(--border-color)">
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
export class PatientFormComponent {
  private readonly i18n = inject(I18nService);

  t(key: string): string {
    return this.i18n.t(key);
  }

  readonly labels = input.required<PatientFormLabels>();
  readonly loading = input(false);
  readonly error = input<string | null>(null);

  readonly fullName = model('');
  readonly gender = model('');
  readonly birthDate = model('');
  readonly phone = model('');
  readonly city = model('');
  readonly district = model('');
  readonly address = model('');
  readonly emergencyContactName = model('');
  readonly emergencyContactPhone = model('');
  readonly allergies = model('');
  readonly medicalHistory = model('');
  readonly bloodGroup = model('');
  readonly email = model('');

  readonly submitted = output<void>();
  readonly cancelled = output<void>();

  onGenderChange(event: Event): void {
    const value = (event.target as HTMLSelectElement).value;
    this.gender.set(value);
  }

  onBloodGroupChange(event: Event): void {
    const value = (event.target as HTMLSelectElement).value;
    this.bloodGroup.set(value);
  }
}
