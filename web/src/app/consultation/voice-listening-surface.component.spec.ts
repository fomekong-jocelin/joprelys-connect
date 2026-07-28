import { ComponentFixture, TestBed } from '@angular/core/testing';
import { VoiceListeningSurfaceComponent } from './voice-listening-surface.component';

describe('VoiceListeningSurfaceComponent', () => {
  let fixture: ComponentFixture<VoiceListeningSurfaceComponent>;
  let component: VoiceListeningSurfaceComponent;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [VoiceListeningSurfaceComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(VoiceListeningSurfaceComponent);
    component = fixture.componentInstance;
    component.active = true;
    component.audioLevel = 0.7;
    component.badgeText = 'IA en cours...';
    component.stopText = 'Arrêter';
    component.statusText = 'Écoute en cours... Parlez naturellement';
    component.tipText = 'Conseil : dictez naturellement.';
    component.accessibleLabel = 'Écoute vocale active';
    fixture.detectChanges();
  });

  it('renders the shared listening hierarchy from inputs', () => {
    const text = fixture.nativeElement.textContent;
    expect(text).toContain('IA en cours...');
    expect(text).toContain('Arrêter');
    expect(text).toContain('Écoute en cours... Parlez naturellement');
    expect(text).toContain('Conseil : dictez naturellement.');
    expect(fixture.nativeElement.querySelector('[data-testid="voice-listening-surface"]')).not.toBeNull();
  });

  it('emits stop without owning engine behavior', () => {
    const stop = vi.fn();
    component.stop.subscribe(stop);

    fixture.nativeElement.querySelector('[data-testid="voice-listening-stop"]').click();

    expect(stop).toHaveBeenCalledTimes(1);
  });

  it('disables the stop action when the consumer requests it', () => {
    fixture.componentRef.setInput('stopDisabled', true);
    fixture.detectChanges();

    const button = fixture.nativeElement.querySelector(
      '[data-testid="voice-listening-stop"]',
    ) as HTMLButtonElement;
    expect(button.disabled).toBe(true);
  });
});
