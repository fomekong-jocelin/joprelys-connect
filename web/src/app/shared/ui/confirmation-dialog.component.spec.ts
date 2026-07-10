import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ConfirmationDialogComponent } from './confirmation-dialog.component';

describe('ConfirmationDialogComponent', () => {
  let component: ConfirmationDialogComponent;
  let fixture: ComponentFixture<ConfirmationDialogComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({ imports: [ConfirmationDialogComponent] }).compileComponents();
    fixture = TestBed.createComponent(ConfirmationDialogComponent);
    component = fixture.componentInstance;
    component.visible = true;
    component.title = 'Annuler la facture';
    component.message = 'Confirmation requise';
    component.confirmLabel = 'Confirmer';
    component.cancelLabel = 'Retour';
    fixture.detectChanges();
  });

  it('emits confirmation only after the destructive action is clicked', () => {
    const confirmed = vi.fn();
    component.confirmed.subscribe(confirmed);
    const button = fixture.nativeElement.querySelector('.confirmation-dialog__confirm') as HTMLButtonElement;
    button.click();
    expect(confirmed).toHaveBeenCalledOnce();
  });

  it('emits cancellation when Escape is pressed', () => {
    const cancelled = vi.fn();
    component.cancelled.subscribe(cancelled);
    document.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape' }));
    expect(cancelled).toHaveBeenCalledOnce();
  });

  it('blocks actions while a request is pending', () => {
    const confirmed = vi.fn();
    const cancelled = vi.fn();
    component.confirmed.subscribe(confirmed);
    component.cancelled.subscribe(cancelled);
    component.busy = true;
    fixture.detectChanges();
    component.requestConfirm();
    component.requestCancel();
    expect(confirmed).not.toHaveBeenCalled();
    expect(cancelled).not.toHaveBeenCalled();
  });
});
