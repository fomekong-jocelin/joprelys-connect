import { Component, OnInit, inject, input } from '@angular/core';
import { FormGroup, ReactiveFormsModule } from '@angular/forms';
import { I18nService } from '../core/i18n/i18n.service';
import { VISIT_ORIENTATION_CODES } from '../visit/visit-orientation.util';
import { OTHER_SERVICE, VisitAdmissionOptionsService } from './visit-admission-options.service';

/**
 * Champs d'une visite (motif, orientation, service, praticien principal, arrivée),
 * identiques quel que soit le point d'entrée de l'admission.
 */
@Component({
  selector: 'app-visit-details-fields',
  standalone: true,
  imports: [ReactiveFormsModule],
  providers: [VisitAdmissionOptionsService],
  template: `
    <div class="grid grid-cols-1 gap-3 sm:grid-cols-2" [formGroup]="form()">
      <div class="sm:col-span-2">
        <label class="ui-label mb-1.5" for="visit-reason">{{ text('reason') }} *</label>
        <textarea id="visit-reason" formControlName="reason" class="ui-input min-h-20 resize-y py-2.5"></textarea>
      </div>
      <div>
        <label class="ui-label mb-1.5" for="visit-orientation">{{ text('orientation') }} *</label>
        <select id="visit-orientation" formControlName="orientation" class="ui-input">
          @for (code of orientations; track code) {
            <option [value]="code">{{ text('orientationOption.' + code) }}</option>
          }
        </select>
      </div>
      <div>
        <label class="ui-label mb-1.5" for="visit-service">{{ text('service') }}</label>
        <select id="visit-service" formControlName="service" class="ui-input" (change)="resetPractitionerIfOutOfService()">
          <option value="">{{ text('serviceSelectPlaceholder') }}</option>
          @for (service of options.services(); track service) {
            <option [value]="service">{{ service }}</option>
          }
          <option [value]="otherService">{{ text('serviceOptionOther') }}</option>
        </select>
      </div>
      @if (form().get('orientation')?.value === 'OTHER') {
        <div class="sm:col-span-2">
          <label class="ui-label mb-1.5" for="visit-custom-orientation">{{ text('orientationOption.OTHER') }}</label>
          <input id="visit-custom-orientation" formControlName="customOrientation" class="ui-input" />
        </div>
      }
      @if (form().get('service')?.value === otherService) {
        <div class="sm:col-span-2">
          <label class="ui-label mb-1.5" for="visit-custom-service">{{ text('customServicePlaceholder') }}</label>
          <input id="visit-custom-service" formControlName="customService" class="ui-input" />
        </div>
      }
      <div>
        <label class="ui-label mb-1.5" for="visit-practitioner">{{ text('mainPractitioner') }}</label>
        <select id="visit-practitioner" formControlName="mainPractitionerId" class="ui-input">
          <option value="">{{ text('mainPractitionerPlaceholder') }}</option>
          @for (practitioner of practitioners(); track practitioner.id) {
            <option [value]="practitioner.id">{{ practitioner.displayName }}</option>
          }
        </select>
      </div>
      <div>
        <label class="ui-label mb-1.5" for="visit-arrival">{{ text('arrivalAt') }}</label>
        <input id="visit-arrival" type="datetime-local" formControlName="arrivalAt" class="ui-input" />
      </div>
    </div>
  `,
})
export class VisitDetailsFieldsComponent implements OnInit {
  private readonly i18n = inject(I18nService);
  readonly options = inject(VisitAdmissionOptionsService);

  readonly form = input.required<FormGroup>();
  readonly orientations = VISIT_ORIENTATION_CODES;
  readonly otherService = OTHER_SERVICE;

  ngOnInit(): void {
    this.options.load();
  }

  text(key: string): string {
    return this.i18n.t(`admission.${key}`);
  }

  practitioners() {
    return this.options.practitionersFor(this.form().get('service')?.value);
  }

  resetPractitionerIfOutOfService(): void {
    const control = this.form().get('mainPractitionerId');
    if (control?.value && !this.practitioners().some((member) => member.id === control.value)) {
      control.setValue('');
    }
  }
}
