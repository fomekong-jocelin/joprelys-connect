import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, OnChanges, Output, SimpleChanges, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Receivable, ReceivableReminder } from '../../patient/patient.models';
import { BillingApiService } from '../../patient/billing-api.service';
import { IconComponent } from '../../shared/ui/icon.component';
import { I18nService } from '../../core/i18n/i18n.service';

export interface BillingReminderForm {
  actionType: 'PHONE_CALL' | 'EMAIL' | 'LETTER' | 'VISIT';
  status: 'PENDING' | 'PROMISED_PAYMENT' | 'DISPUTE' | 'UNREACHABLE';
  notes: string;
}

@Component({
  selector: 'app-billing-reminder-modal',
  standalone: true,
  imports: [CommonModule, FormsModule, IconComponent],
  template: `
    @if (visible && receivable) {
      <div class="fixed inset-0 bg-black/60 backdrop-blur-xs flex items-center justify-center p-4 z-50">
        <div class="ui-card max-w-2xl w-full p-6 space-y-4 animate-in fade-in zoom-in-95 duration-150 flex flex-col max-h-[90vh]">
          <!-- Header -->
          <div class="flex justify-between items-center border-b border-[var(--app-border)]/40 pb-2 shrink-0">
            <h3 class="font-bold text-xs text-[var(--text-primary)] uppercase tracking-wider flex items-center gap-1.5">
              <app-ui-icon name="information-circle" />
              Consigner une action de relance
            </h3>
            <button (click)="close.emit()" [attr.aria-label]="t('common.aria.close', 'Fermer')" class="text-xs text-[var(--text-muted)] font-bold p-1 rounded-[var(--radius-brand-sm)] hover:bg-[var(--app-surface-muted)]">
              <app-ui-icon name="x-mark" class="text-base" />
            </button>
          </div>

          <div class="flex gap-6 overflow-hidden grow">
            <!-- Left Panel: Form -->
            <div class="w-1/2 space-y-3 flex flex-col justify-between overflow-y-auto pr-2">
              <div class="space-y-3">
                <div class="text-[11px] space-y-1 bg-[var(--app-surface-muted)] p-3 rounded-sm border border-[var(--app-border)]/40">
                  <div>Débiteur : <strong class="font-mono text-[var(--text-primary)]">{{ receivable.debtorType }} ({{ receivable.debtorId }})</strong></div>
                  <div>Reste à recouvrer : <strong class="text-rose-600">{{ receivable.remainingAmount | number:'1.0-0' }} FCFA</strong></div>
                </div>

                <div>
                  <label class="text-[10px] font-bold text-[var(--text-secondary)] block mb-1">Moyen de relance :</label>
                  <select [(ngModel)]="actionType" class="ui-select text-xs w-full">
                    <option value="PHONE_CALL">Appel téléphonique (PHONE_CALL)</option>
                    <option value="EMAIL">E-mail (EMAIL)</option>
                    <option value="LETTER">Courrier postal (LETTER)</option>
                    <option value="VISIT">Visite physique (VISIT)</option>
                  </select>
                </div>

                <div>
                  <label class="text-[10px] font-bold text-[var(--text-secondary)] block mb-1">Statut obtenu :</label>
                  <select [(ngModel)]="status" class="ui-select text-xs w-full">
                    <option value="PENDING">En attente (PENDING)</option>
                    <option value="PROMISED_PAYMENT">Promesse de paiement (PROMISED_PAYMENT)</option>
                    <option value="DISPUTE">Litige / Contestation (DISPUTE)</option>
                    <option value="UNREACHABLE">Injoignable (UNREACHABLE)</option>
                  </select>
                </div>

                <div>
                  <label class="text-[10px] font-bold text-[var(--text-secondary)] block mb-1">Commentaires / Notes :</label>
                  <textarea [(ngModel)]="notes" rows="4" placeholder="Saisir les détails de la relance..." class="ui-input text-xs w-full resize-none"></textarea>
                </div>
              </div>

              <div class="flex justify-end gap-2 pt-2 border-t border-[var(--app-border)]/40 shrink-0">
                <button (click)="close.emit()" class="ui-button ui-button-secondary text-xs">
                  Annuler
                </button>
                <button (click)="submit()" [disabled]="saving || !notes.trim()" class="ui-button ui-button-primary text-xs disabled:opacity-50 flex items-center gap-1">
                  <app-ui-icon name="check" />
                  Consigner la relance
                </button>
              </div>
            </div>

            <!-- Right Panel: History timeline -->
            <div class="w-1/2 border-l border-[var(--app-border)]/40 pl-6 flex flex-col overflow-hidden">
              <h4 class="font-bold text-[10px] text-[var(--text-secondary)] uppercase tracking-wider mb-3 shrink-0">
                Historique des relances ({{ reminders().length }})
              </h4>
              <div class="overflow-y-auto grow space-y-4 pr-1">
                @if (loadingReminders()) {
                  <p class="text-center text-[10px] text-[var(--text-muted)] py-6 italic">Chargement de l'historique...</p>
                } @else if (reminders().length === 0) {
                  <p class="text-center text-[10px] text-[var(--text-muted)] py-6 italic">Aucune relance consignée pour cette créance.</p>
                } @else {
                  <div class="relative border-l border-brand-cyan/20 ml-2 space-y-4 py-1">
                    @for (rem of reminders(); track rem.id) {
                      <div class="relative pl-6">
                        <!-- Timeline bullet -->
                        <span class="absolute -left-[5px] top-1.5 w-2.5 h-2.5 rounded-full border-2 border-brand-cyan bg-[var(--app-surface)]"></span>
                        
                        <div class="bg-[var(--app-surface-muted)]/40 p-2.5 rounded-sm border border-[var(--app-border)]/20 text-xs space-y-1">
                          <div class="flex justify-between items-center">
                            <span class="px-1.5 py-0.5 text-[8px] font-bold rounded-sm bg-brand-cyan/10 text-brand-cyan">
                              {{ rem.actionType }}
                            </span>
                            <span class="text-[9px] text-[var(--text-muted)]">
                              {{ rem.createdAt | date:'dd/MM/yyyy HH:mm' }}
                            </span>
                          </div>
                          
                          <div class="text-[10px]">
                            Statut : 
                            <span class="font-bold"
                                  [class.text-amber-600]="rem.status === 'PENDING'"
                                  [class.text-emerald-600]="rem.status === 'PROMISED_PAYMENT'"
                                  [class.text-red-600]="rem.status === 'DISPUTE'"
                                  [class.text-purple-600]="rem.status === 'UNREACHABLE'">
                              {{ rem.status }}
                            </span>
                          </div>

                          @if (rem.notes) {
                            <p class="text-[10px] text-[var(--text-secondary)] italic bg-[var(--app-surface)] p-1.5 rounded-sm mt-1 border border-[var(--app-border)]/20">
                              {{ rem.notes }}
                            </p>
                          }
                        </div>
                      </div>
                    }
                  </div>
                }
              </div>
            </div>
          </div>
        </div>
      </div>
    }
  `,
})
export class BillingReminderModalComponent implements OnChanges {
  private readonly billingApi = inject(BillingApiService);
  private readonly i18n = inject(I18nService);

  t(key: string, defaultValue: string): string {
    return this.i18n.t(key, defaultValue);
  }

  @Input({ required: true }) visible = false;
  @Input() receivable: Receivable | null = null;
  @Input({ required: true }) saving = false;

  @Output() close = new EventEmitter<void>();
  @Output() submitReminder = new EventEmitter<BillingReminderForm>();

  actionType: 'PHONE_CALL' | 'EMAIL' | 'LETTER' | 'VISIT' = 'PHONE_CALL';
  status: 'PENDING' | 'PROMISED_PAYMENT' | 'DISPUTE' | 'UNREACHABLE' = 'PENDING';
  notes = '';

  reminders = signal<ReceivableReminder[]>([]);
  loadingReminders = signal<boolean>(false);

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['receivable'] && this.receivable) {
      this.actionType = 'PHONE_CALL';
      this.status = 'PENDING';
      this.notes = '';
      this.loadReminderHistory();
    }
  }

  loadReminderHistory(): void {
    if (!this.receivable) {
      return;
    }
    this.loadingReminders.set(true);
    this.billingApi.getReminders(this.receivable.id).subscribe({
      next: (data) => {
        this.reminders.set(data);
        this.loadingReminders.set(false);
      },
      error: () => {
        this.loadingReminders.set(false);
      }
    });
  }

  submit(): void {
    if (!this.notes.trim()) {
      return;
    }
    this.submitReminder.emit({
      actionType: this.actionType,
      status: this.status,
      notes: this.notes.trim()
    });
  }
}
