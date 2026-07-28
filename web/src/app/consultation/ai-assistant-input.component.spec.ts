import { ComponentFixture, TestBed } from '@angular/core/testing';
import { I18nService } from '../core/i18n/i18n.service';
import { AiAssistantInputComponent } from './ai-assistant-input.component';

describe('AiAssistantInputComponent unified listening surface', () => {
  let fixture: ComponentFixture<AiAssistantInputComponent>;
  let component: AiAssistantInputComponent;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AiAssistantInputComponent],
      providers: [{
        provide: I18nService,
        useValue: {
          t: (_key: string, fallback?: string) => fallback ?? _key,
        },
      }],
    }).compileComponents();

    fixture = TestBed.createComponent(AiAssistantInputComponent);
    component = fixture.componentInstance;
    component.conversationMode = false;
    component.recording = true;
    component.audioLevel = 0.6;
    fixture.detectChanges();
  });

  it('uses the shared listening surface while classic dictation records', () => {
    expect(fixture.nativeElement.querySelector('app-voice-listening-surface')).not.toBeNull();
    expect(fixture.nativeElement.textContent).toContain(
      'Écoute en cours... Parlez naturellement',
    );
  });

  it('maps the shared stop action to the dictation toggle', () => {
    const toggle = vi.fn();
    component.toggleRecording.subscribe(toggle);

    fixture.nativeElement.querySelector('[data-testid="voice-listening-stop"]').click();

    expect(toggle).toHaveBeenCalledTimes(1);
  });
});
