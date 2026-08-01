import { FormBuilder } from '@angular/forms';
import { provideHttpClient } from '@angular/common/http';
import { ActivatedRoute, provideRouter } from '@angular/router';
import { ComponentFixture, TestBed } from '@angular/core/testing';
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
  let fixture: ComponentFixture<ConsultationComponent>;
  let component: ConsultationComponent;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ConsultationComponent],
      providers: [
        FormBuilder,
        provideHttpClient(),
        provideRouter([]),
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { paramMap: { get: () => 'visit-1' } } },
        },
        { provide: ConsultationApiService, useValue: {} },
        { provide: VisitApiService, useValue: {} },
        { provide: LabOrderApiService, useValue: {} },
        { provide: I18nService, useClass: I18nStub },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ConsultationComponent);
    component = fixture.componentInstance;
    component.form.patchValue({
      symptoms: 'Fièvre',
      diagnosis: 'Diagnostic provisoire',
      advice: 'Ancien conseil',
    });
  });

  it('conserve les champs acceptés absents du nouveau brouillon', () => {
    component.applyAiDraft({
      symptoms: 'Fièvre avec céphalées',
      advice: 'Hydratation',
    });

    expect(component.form.get('symptoms')?.value).toBe('Fièvre avec céphalées');
    expect(component.form.get('advice')?.value).toBe('Hydratation');
    expect(component.form.get('diagnosis')?.value).toBe('Diagnostic provisoire');
    expect(component.form.dirty).toBe(true);
    expect(component.form.invalid).toBe(false);
  });

  it('applique un unique diagnostic sans contrôles legacy', () => {
    component.applyAiDraft({
      diagnosis: 'Diagnostic documenté',
    });

    expect(component.form.get('diagnosis')?.value).toBe('Diagnostic documenté');
    expect(component.form.contains('suspectedDiagnosis')).toBe(false);
    expect(component.form.contains('finalDiagnosis')).toBe(false);
  });
});
