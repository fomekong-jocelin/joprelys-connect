import { ComponentFixture, TestBed } from '@angular/core/testing';
import { I18nService } from '../core/i18n/i18n.service';
import { AiAssistantInputComponent } from './ai-assistant-input.component';

describe('AiAssistantInputComponent', () => {
  let fixture: ComponentFixture<AiAssistantInputComponent>;
  let component: AiAssistantInputComponent;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AiAssistantInputComponent],
      providers: [{
        provide: I18nService,
        useValue: { t: (key: string, fallback?: string) => fallback ?? key },
      }],
    }).compileComponents();

    fixture = TestBed.createComponent(AiAssistantInputComponent);
    component = fixture.componentInstance;
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  it('should rearm classic conversational recording after assistant speech ends', () => {
    vi.useFakeTimers();
    const toggle = vi.fn();
    component.toggleRecording.subscribe(toggle);

    fixture.componentRef.setInput('conversationMode', true);
    fixture.componentRef.setInput('mediaRecorderSupported', true);
    fixture.componentRef.setInput('busy', false);
    fixture.componentRef.setInput('blocked', false);
    fixture.componentRef.setInput('recording', false);
    fixture.componentRef.setInput('speaking', true);
    fixture.detectChanges();

    fixture.componentRef.setInput('speaking', false);
    fixture.detectChanges();
    vi.advanceTimersByTime(649);
    expect(toggle).not.toHaveBeenCalled();

    vi.advanceTimersByTime(1);
    expect(toggle).toHaveBeenCalledTimes(1);
  });

  it('should not rearm while validation blocks the conversation', () => {
    vi.useFakeTimers();
    const toggle = vi.fn();
    component.toggleRecording.subscribe(toggle);

    fixture.componentRef.setInput('conversationMode', true);
    fixture.componentRef.setInput('mediaRecorderSupported', true);
    fixture.componentRef.setInput('blocked', true);
    fixture.componentRef.setInput('speaking', true);
    fixture.detectChanges();

    fixture.componentRef.setInput('speaking', false);
    fixture.detectChanges();
    vi.advanceTimersByTime(1000);

    expect(toggle).not.toHaveBeenCalled();
  });

  it('should cancel its safety timer when recording already restarted upstream', () => {
    vi.useFakeTimers();
    const toggle = vi.fn();
    component.toggleRecording.subscribe(toggle);

    fixture.componentRef.setInput('conversationMode', true);
    fixture.componentRef.setInput('mediaRecorderSupported', true);
    fixture.componentRef.setInput('speaking', true);
    fixture.detectChanges();
    fixture.componentRef.setInput('speaking', false);
    fixture.detectChanges();

    vi.advanceTimersByTime(300);
    fixture.componentRef.setInput('recording', true);
    fixture.detectChanges();
    vi.advanceTimersByTime(500);

    expect(toggle).not.toHaveBeenCalled();
  });
});
