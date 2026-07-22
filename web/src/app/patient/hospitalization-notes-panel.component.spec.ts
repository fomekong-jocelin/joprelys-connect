import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { I18nService } from '../core/i18n/i18n.service';
import { RbacApiService } from '../clinic/rbac/rbac-api.service';
import { PatientApiService } from './patient-api.service';
import { HospitalizationNotesPanelComponent } from './hospitalization-notes-panel.component';

describe('HospitalizationNotesPanelComponent', () => {
  let fixture: ComponentFixture<HospitalizationNotesPanelComponent>;
  let api: { getHospitalizationNotes: ReturnType<typeof vi.fn>; addHospitalizationNote: ReturnType<typeof vi.fn> };
  let permissions: Set<string>;

  beforeEach(async () => {
    permissions = new Set(['HOSPITALIZATION_NOTE_WRITE']);
    api = {
      getHospitalizationNotes: vi.fn().mockReturnValue(of([{ id: 'note-1', hospitalizationId: 'stay-1', noteContent: 'Patient stable', authorName: 'Infirmier', createdAt: '2026-07-10T08:00:00Z' }])),
      addHospitalizationNote: vi.fn().mockReturnValue(of({})),
    };

    await TestBed.configureTestingModule({
      imports: [HospitalizationNotesPanelComponent],
      providers: [
        { provide: PatientApiService, useValue: api },
        { provide: RbacApiService, useValue: { hasPermission: (permission: string) => permissions.has(permission) } },
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

  it('saves a new note with the dedicated permission', () => {
    fixture.componentInstance.noteContent.set('Surveillance poursuivie');
    fixture.componentInstance.save(new Event('submit'));

    expect(api.addHospitalizationNote).toHaveBeenCalledWith('stay-1', 'Surveillance poursuivie');
  });

  it('blocks note creation without the dedicated permission', () => {
    permissions.clear();
    fixture.detectChanges();
    fixture.componentInstance.noteContent.set('Surveillance poursuivie');
    fixture.componentInstance.save(new Event('submit'));

    expect(api.addHospitalizationNote).not.toHaveBeenCalled();
    expect(fixture.nativeElement.querySelector('#stay-note')).toBeNull();
  });
});
