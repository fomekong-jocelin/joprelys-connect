import { CommonModule, DatePipe } from '@angular/common';
import { Component, inject, input, output, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { I18nService } from '../../core/i18n/i18n.service';
import { ButtonComponent } from '../../shared/ui/button.component';
import {
  CreateEmergencyBelongingRequest,
  EmergencyBelonging,
  EmergencyBelongingTransferAction,
  TransferEmergencyBelongingRequest,
} from './emergency-medico-legal.models';

export interface BelongingTransferCommand {
  belongingId: string;
  request: TransferEmergencyBelongingRequest;
}

@Component({
  selector: 'app-emergency-belongings-section',
  standalone: true,
  imports: [CommonModule, DatePipe, ReactiveFormsModule, ButtonComponent],
  templateUrl: './emergency-belongings-section.component.html',
})
export class EmergencyBelongingsSectionComponent {
  private readonly fb = inject(FormBuilder);
  readonly i18n = inject(I18nService);

  readonly belongings = input.required<EmergencyBelonging[]>();
  readonly disabled = input(false);
  readonly itemSubmitted = output<CreateEmergencyBelongingRequest>();
  readonly transferSubmitted = output<BelongingTransferCommand>();

  readonly showItemForm = signal(false);
  readonly transferBelongingId = signal<string | null>(null);

  readonly itemForm = this.fb.nonNullable.group({
    category: ['PERSONAL_ITEM', Validators.required],
    description: ['', [Validators.required, Validators.maxLength(500)]],
    quantity: [1, [Validators.required, Validators.min(1)]],
    itemCondition: ['', Validators.maxLength(255)],
    sealNumber: ['', Validators.maxLength(80)],
    depositedByName: ['', Validators.maxLength(160)],
  });

  readonly transferForm = this.fb.nonNullable.group({
    action: ['TRANSFERRED'],
    fromCustodian: ['', Validators.maxLength(160)],
    recipientName: ['', Validators.maxLength(160)],
    recipientIdDocument: ['', Validators.maxLength(120)],
    notes: ['', Validators.maxLength(1000)],
  });

  toggleItemForm(): void {
    this.showItemForm.update(value => !value);
  }

  openTransfer(belongingId: string): void {
    this.transferBelongingId.set(belongingId);
    this.transferForm.reset({
      action: 'TRANSFERRED',
      fromCustodian: '',
      recipientName: '',
      recipientIdDocument: '',
      notes: '',
    });
  }

  closeTransfer(): void {
    this.transferBelongingId.set(null);
  }

  submitItem(): void {
    if (this.itemForm.invalid || this.disabled()) {
      this.itemForm.markAllAsTouched();
      return;
    }
    const value = this.itemForm.getRawValue();
    this.itemSubmitted.emit({
      category: value.category,
      description: value.description.trim(),
      quantity: value.quantity,
      itemCondition: this.optional(value.itemCondition),
      sealNumber: this.optional(value.sealNumber),
      depositedByName: this.optional(value.depositedByName),
    });
    this.itemForm.reset({
      category: 'PERSONAL_ITEM',
      description: '',
      quantity: 1,
      itemCondition: '',
      sealNumber: '',
      depositedByName: '',
    });
    this.showItemForm.set(false);
  }

  submitTransfer(): void {
    const belongingId = this.transferBelongingId();
    if (!belongingId || this.transferForm.invalid || this.disabled()) {
      this.transferForm.markAllAsTouched();
      return;
    }
    const value = this.transferForm.getRawValue();
    if (this.requiresRecipientIdentity()
      && (!value.recipientName.trim() || !value.recipientIdDocument.trim())) {
      this.transferForm.markAllAsTouched();
      return;
    }
    this.transferSubmitted.emit({
      belongingId,
      request: {
        action: value.action as EmergencyBelongingTransferAction,
        fromCustodian: this.optional(value.fromCustodian),
        recipientName: this.optional(value.recipientName),
        recipientIdDocument: this.optional(value.recipientIdDocument),
        notes: this.optional(value.notes),
      },
    });
    this.closeTransfer();
  }

  requiresRecipientIdentity(): boolean {
    const action = this.transferForm.controls.action.value;
    return action === 'RELEASED' || action === 'RETURNED';
  }

  canTransfer(item: EmergencyBelonging): boolean {
    return item.custodyStatus !== 'RELEASED' && item.custodyStatus !== 'DISPOSED';
  }

  t(key: string): string {
    return this.i18n.t(key);
  }

  private optional(value: string): string | undefined {
    const normalized = value.trim();
    return normalized || undefined;
  }
}
