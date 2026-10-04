import { ComponentFixture, TestBed } from '@angular/core/testing';
import { I18nService } from '../core/i18n/i18n.service';
import { ConsultationEntryModeComponent } from './consultation-entry-mode.component';

describe('Consultation entry mode selection', () => {
  let fixture: ComponentFixture<ConsultationEntryModeComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ConsultationEntryModeComponent],
      providers: [{ provide: I18nService, useValue: { t: (key: string) => key } }],
    }).compileComponents();
    fixture = TestBed.createComponent(ConsultationEntryModeComponent);
    fixture.componentRef.setInput('visitAvailable', true);
    fixture.detectChanges();
  });

  for (const mode of ['manual', 'dictation', 'conversation'] as const) {
    it(`selects ${mode} without starting capture and emits only after the action`, () => {
      const start = vi.fn();
      fixture.componentInstance.start.subscribe(start);
      const radio: HTMLInputElement = fixture.nativeElement.querySelector(`input[value="${mode}"]`);
      radio.click();
      fixture.detectChanges();

      expect(start).not.toHaveBeenCalled();
      expect(radio.checked).toBe(true);
      expect(fixture.nativeElement.querySelector('h3').textContent).toContain(`.${mode}.title`);
      fixture.nativeElement.querySelector('[data-testid="start-consultation-mode"]').click();
      expect(start).toHaveBeenCalledExactlyOnceWith(mode);
    });
  }

  it('disables unavailable dictation and explains why', () => {
    fixture.componentRef.setInput('dictationSupported', false);
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('input[value="dictation"]').disabled).toBe(true);
    expect(fixture.nativeElement.textContent).toContain('consultation.entry.dictationUnavailable');
    const start = vi.fn();
    fixture.componentInstance.start.subscribe(start);
    fixture.componentInstance.selected.set('dictation');
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('button').disabled).toBe(true);
    fixture.componentInstance.startSelected();
    expect(start).not.toHaveBeenCalled();
  });

  it('blocks selection and duplicate actions while initializing', () => {
    fixture.componentRef.setInput('busy', true);
    fixture.detectChanges();
    const start = vi.fn();
    fixture.componentInstance.start.subscribe(start);
    fixture.componentInstance.select('manual');
    fixture.componentInstance.startSelected();
    expect(fixture.componentInstance.selected()).toBe('conversation');
    expect(fixture.nativeElement.querySelector('button').disabled).toBe(true);
    expect(start).not.toHaveBeenCalled();
  });

  it('blocks starting without a visit', () => {
    fixture.componentRef.setInput('visitAvailable', false);
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('button').disabled).toBe(true);
    const start = vi.fn();
    fixture.componentInstance.start.subscribe(start);
    fixture.componentInstance.startSelected();
    expect(start).not.toHaveBeenCalled();
  });
});
