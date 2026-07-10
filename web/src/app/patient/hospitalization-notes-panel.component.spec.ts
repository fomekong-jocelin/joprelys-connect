import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { I18nService } from '../core/i18n/i18n.service';
import { PatientApiService } from './patient-api.service';
import { HospitalizationNotesPanelComponent } from './hospitalization-notes-panel.component';

describe('HospitalizationNotesPanelComponent', () => {
  let fixture: ComponentFixture<HospitalizationNotesPanelComponent>;
  let api: { getHospitalizationNotes: ReturnType<typeof vi.fn>; addHospitalizationNote: ReturnType<typeof vi.fn> };

  beforeEach(async () => {
    api = {
      getHospitalizationNotes: vi.fn().mockReturnValue(of([{ id: 'note-1', hospitalizationId: 'stay-1', noteContent: 'Patient stable', authorName: 'Infirmier', createdAt: '2026-07-10T08:00:00Z' }])),
      addHospitalizationNote: vi.fn().mockReturnValue(of({})),
    };

    await TestBed.configureTestingModule({
      imports: [HospitalizationNotesPanelComponent],
      providers: [
        { provide: PatientApiService, useValue: api },
        { provide: I18nService, useValue: { t: (_key: string, fallback: string) => fallback } },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(HospitalizationNotesPanelComponent);
    fixture.componentRef.setInput('hospitalizationId', 'stay-1');
    fixture.componentRef.setInput('canModify', true);
    fixture.detectChanges();
  });

  it('loads the signed chronological notes for the stay', () => {
    expect(api.getHospitalizationNotes).toHaveBeenCalledWith('stay-1');
    expect(fixture.nativeElement.textContent).toContain('Patient stable');
    expect(fixture.nativeElement.textContent).toContain('Infirmier');
  });

  it('saves a new note for the active stay', () => {
    fixture.componentInstance.noteContent.set('Surveillance poursuivie');
    fixture.componentInstance.save(new Event('submit'));

    expect(api.addHospitalizationNote).toHaveBeenCalledWith('stay-1', 'Surveillance poursuivie');
  });
});
