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
    { id: 'item-1', text: 'Température trente-huit cinq', timestamp: 1_700_000_000_000 },
  ];

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [RealtimeTranscriptHistoryComponent],
      providers: [{
        provide: I18nService,
        useValue: { t: (key: string) => key },
      }],
    }).compileComponents();

    fixture = TestBed.createComponent(RealtimeTranscriptHistoryComponent);
    component = fixture.componentInstance;
    fixture.componentRef.setInput('entries', entries);
    fixture.componentRef.setInput('connected', true);
    fixture.detectChanges();
  });

  it('lets the clinician edit and submit the latest transcript', () => {
    const emitted = vi.fn();
    component.correct.subscribe(emitted);

    const buttons = fixture.nativeElement.querySelectorAll('button') as NodeListOf<HTMLButtonElement>;
    buttons[0].click();
    fixture.detectChanges();

    const textarea = fixture.nativeElement.querySelector('textarea') as HTMLTextAreaElement;
    textarea.value = 'Température 38,5 degrés';
    textarea.dispatchEvent(new Event('input'));
    fixture.detectChanges();

    const actionButtons = fixture.nativeElement.querySelectorAll('button') as NodeListOf<HTMLButtonElement>;
    actionButtons[0].click();

    expect(emitted).toHaveBeenCalledWith('Température 38,5 degrés');
    expect(component.editingId()).toBeNull();
  });

  it('does not open the correction editor while the pipeline is busy', () => {
    fixture.componentRef.setInput('correctionDisabled', true);
    fixture.detectChanges();

    const button = fixture.nativeElement.querySelector('button') as HTMLButtonElement;
    expect(button.disabled).toBe(true);
    button.click();

    expect(component.editingId()).toBeNull();
    expect(fixture.nativeElement.querySelector('textarea')).toBeNull();
  });
});
