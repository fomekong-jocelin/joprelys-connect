import { ApplicationRef, EnvironmentInjector } from '@angular/core';
import { FormBuilder } from '@angular/forms';
import { provideHttpClient } from '@angular/common/http';
import { ActivatedRoute, Router } from '@angular/router';
import { TestBed } from '@angular/core/testing';
import { I18nService } from '../core/i18n/i18n.service';
import { LabOrderApiService } from '../clinic/lab/lab-api.service';
import { VisitApiService } from '../visit/visit-api.service';
import { ConsultationApiService } from './consultation-api.service';
import { ConsultationComponent } from './consultation.component';

class I18nStub {
  t(_key: string, defaultValue?: string): string {
    return defaultValue ?? _key;
  }
}

describe('ConsultationComponent AI draft application', () => {
  let component: ConsultationComponent;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        FormBuilder,
        provideHttpClient(),
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { paramMap: { get: () => 'visit-1' } } },
        },
        { provide: Router, useValue: { navigate: () => Promise.resolve(true) } },
        { provide: ApplicationRef, useValue: { attachView: () => {}, detachView: () => {} } },
        { provide: EnvironmentInjector, useValue: {} },
        { provide: ConsultationApiService, useValue: {} },
        { provide: VisitApiService, useValue: {} },
        { provide: LabOrderApiService, useValue: {} },
        { provide: I18nService, useClass: I18nStub },
      ],
    });

    component = TestBed.runInInjectionContext(() => new ConsultationComponent());
    component.form.patchValue({
      symptoms: 'Fièvre',
      diagnosis: 'Diagnostic provisoire',
      advice: 'Ancien conseil',
    });
  });

  it('vide les champs absents du brouillon accepté', () => {
    component.applyAiDraft({
      symptoms: 'Fièvre avec céphalées',
      advice: 'Hydratation',
    });

    expect(component.form.get('symptoms')?.value).toBe('Fièvre avec céphalées');
    expect(component.form.get('advice')?.value).toBe('Hydratation');
    expect(component.form.get('diagnosis')?.value).toBe('');
    expect(component.form.dirty).toBe(true);
    expect(component.form.invalid).toBe(true);
  });
});
