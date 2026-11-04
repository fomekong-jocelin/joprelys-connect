import { ComponentFixture, TestBed } from '@angular/core/testing';
import { DialogHostComponent } from './dialog-host.component';
import { DialogService, DialogTone } from './dialog.service';

describe('DialogHostComponent', () => {
  let fixture: ComponentFixture<DialogHostComponent>;
  let dialog: DialogService;

  beforeEach(() => {
    TestBed.configureTestingModule({ imports: [DialogHostComponent] });
    fixture = TestBed.createComponent(DialogHostComponent);
    dialog = TestBed.inject(DialogService);
    fixture.detectChanges();
  });

  // La décision est renvoyée dans un objet : une Promise renvoyée par une fonction async serait attendue (jamais résolue ici).
  async function openConfirm(tone: DialogTone): Promise<{ decision: Promise<boolean> }> {
    const decision = dialog.confirm({ title: 'Titre', message: 'Message', tone, confirmLabel: 'Fermer le lit', cancelLabel: 'Annuler' });
    fixture.detectChanges();
    await fixture.whenStable();
    await Promise.resolve();
    return { decision };
  }

  it('place le focus initial sur « Annuler » pour une action destructive', async () => {
    const { decision } = await openConfirm('danger');

    expect((document.activeElement as HTMLElement).classList).toContain('app-dialog__cancel');

    (document.activeElement as HTMLButtonElement).click();
    await expect(decision).resolves.toBe(false);
  });

  it('place le focus initial sur le bouton de confirmation pour une action non destructive', async () => {
    await openConfirm('warning');

    expect((document.activeElement as HTMLElement).classList).toContain('app-dialog__confirm');
  });
});
