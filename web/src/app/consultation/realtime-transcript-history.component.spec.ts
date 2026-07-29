import { ComponentFixture, TestBed } from '@angular/core/testing';
import { I18nService } from '../core/i18n/i18n.service';
import {
  RealtimeTranscriptEntry,
  RealtimeTranscriptHistoryComponent,
} from './realtime-transcript-history.component';

describe('RealtimeTranscriptHistoryComponent', () => {
  let fixture: ComponentFixture<RealtimeTranscriptHistoryComponent>;
  let component: RealtimeTranscriptHistoryComponent;

  const entries: RealtimeTranscriptEntry[] = [
    {
      id: 'item-1',
      text: 'Température trente-huit cinq',
      timestamp: 1_700_000_000_000,
      durable: true,
      reviewRequired: true,
      correctionCount: 0,
    },
  ];

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [RealtimeTranscriptHistoryComponent],
      providers: [{
        provide: I18nService,
        useValue: { t: (key: string, fallback?: string) => fallback ?? key },
      }],
    }).compileComponents();

    fixture = TestBed.createComponent(RealtimeTranscriptHistoryComponent);
    component = fixture.componentInstance;
    fixture.componentRef.setInput('entries', entries);
    fixture.componentRef.setInput('connected', true);
    fixture.detectChanges();
  });

  it('lets the clinician edit and submit an exact durable transcript segment', () => {
    const emitted = vi.fn();
    component.correct.subscribe(emitted);

    const button = fixture.nativeElement.querySelector('button') as HTMLButtonElement;
    expect(button.disabled).toBe(false);
    button.click();
    fixture.detectChanges();

    const textarea = fixture.nativeElement.querySelector('textarea') as HTMLTextAreaElement;
    expect(textarea).not.toBeNull();
    textarea.value = 'Température 38,5 degrés';
    textarea.dispatchEvent(new Event('input'));
    fixture.detectChanges();

    const actionButtons = fixture.nativeElement.querySelectorAll('button') as NodeListOf<HTMLButtonElement>;
    actionButtons[0].click();

    expect(emitted).toHaveBeenCalledWith({
      id: 'item-1',
      text: 'Température 38,5 degrés',
    });
    expect(component.editingId()).toBeNull();
  });

  it('does not open the correction editor while the persistence pipeline is busy', () => {
    fixture.componentRef.setInput('correctionDisabled', true);
    fixture.detectChanges();

    const button = fixture.nativeElement.querySelector('button') as HTMLButtonElement;
    expect(button.disabled).toBe(true);
    button.click();

    expect(component.editingId()).toBeNull();
    expect(fixture.nativeElement.querySelector('textarea')).toBeNull();
  });

  it('does not offer a correction action for a transcript not yet durably acknowledged', () => {
    fixture.componentRef.setInput('entries', [{ ...entries[0], durable: false }]);
    fixture.detectChanges();

    const button = fixture.nativeElement.querySelector('button') as HTMLButtonElement;
    expect(button.disabled).toBe(true);
  });
});
