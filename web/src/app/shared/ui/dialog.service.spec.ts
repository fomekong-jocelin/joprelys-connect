import { TestBed } from '@angular/core/testing';
import { DialogService } from './dialog.service';
import { DialogHostComponent } from './dialog-host.component';

describe('DialogService & DialogHostComponent', () => {
  let service: DialogService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [DialogHostComponent],
      providers: [DialogService],
    });
    service = TestBed.inject(DialogService);
  });

  it('resolves confirm promise with true when confirmed', async () => {
    const promise = service.confirm({
      title: 'Reprendre la consultation',
      message: 'Ce patient est déjà en consultation. Le reprendre ?',
      confirmLabel: 'Reprendre',
      cancelLabel: 'Annuler',
      tone: 'warning',
    });

    expect(service.activeDialog()).not.toBeNull();
    expect(service.activeDialog()?.title).toBe('Reprendre la consultation');
    expect(service.activeDialog()?.tone).toBe('warning');
    expect(service.activeDialog()?.isAlert).toBe(false);

    service.close(true);
    const result = await promise;
    expect(result).toBe(true);
    expect(service.activeDialog()).toBeNull();
  });

  it('resolves confirm promise with false when cancelled', async () => {
    const promise = service.confirm({
      title: 'Action sensible',
      message: 'Confirmer ?',
    });

    service.close(false);
    const result = await promise;
    expect(result).toBe(false);
    expect(service.activeDialog()).toBeNull();
  });

  it('resolves alert promise when acknowledged', async () => {
    let resolved = false;
    const promise = service.alert({
      title: 'Information',
      message: 'Opération terminée avec succès.',
      confirmLabel: 'Compris',
      tone: 'info',
    }).then(() => {
      resolved = true;
    });

    expect(service.activeDialog()?.isAlert).toBe(true);
    service.close(true);
    await promise;
    expect(resolved).toBe(true);
    expect(service.activeDialog()).toBeNull();
  });

  it('renders active dialog in DialogHostComponent and responds to clicks', async () => {
    const fixture = TestBed.createComponent(DialogHostComponent);
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('section')).toBeNull();

    const promise = service.confirm({
      title: 'Titre modal',
      message: 'Message modal explicatif',
      confirmLabel: 'Accepter',
      cancelLabel: 'Rejeter',
      tone: 'danger',
    });
    fixture.detectChanges();

    const titleEl = fixture.nativeElement.querySelector('#app-dialog-title');
    const messageEl = fixture.nativeElement.querySelector('#app-dialog-message');
    expect(titleEl?.textContent?.trim()).toBe('Titre modal');
    expect(messageEl?.textContent?.trim()).toBe('Message modal explicatif');

    const confirmBtn = fixture.nativeElement.querySelector('.app-dialog__confirm') as HTMLButtonElement;
    expect(confirmBtn.textContent?.trim()).toBe('Accepter');
    expect(confirmBtn.classList.contains('ui-button-danger')).toBe(true);

    confirmBtn.click();
    const result = await promise;
    expect(result).toBe(true);
  });

  it('closes dialog with false on Escape key in DialogHostComponent', async () => {
    const fixture = TestBed.createComponent(DialogHostComponent);
    fixture.detectChanges();

    const promise = service.confirm({
      title: 'Échapper',
      message: 'Annulable par Escape',
    });
    fixture.detectChanges();

    document.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape' }));
    fixture.detectChanges();

    const result = await promise;
    expect(result).toBe(false);
  });
});
