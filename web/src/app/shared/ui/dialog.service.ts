import { Injectable, signal } from '@angular/core';
import { UiIconName } from './icon.component';

export type DialogTone = 'primary' | 'warning' | 'danger' | 'info';

export interface ConfirmDialogOptions {
  title: string;
  message: string;
  confirmLabel?: string;
  cancelLabel?: string;
  tone?: DialogTone;
  icon?: UiIconName;
}

export interface AlertDialogOptions {
  title: string;
  message: string;
  confirmLabel?: string;
  tone?: DialogTone;
  icon?: UiIconName;
}

export interface ActiveDialog {
  id: number;
  title: string;
  message: string;
  confirmLabel: string;
  cancelLabel?: string;
  tone: DialogTone;
  icon: UiIconName;
  isAlert: boolean;
  resolve: (value: boolean) => void;
}

@Injectable({ providedIn: 'root' })
export class DialogService {
  private nextId = 1;
  readonly activeDialog = signal<ActiveDialog | null>(null);

  /** Ouvre une boîte de confirmation personnalisée et renvoie une Promise résolue avec le choix de l'utilisateur. */
  confirm(options: ConfirmDialogOptions): Promise<boolean> {
    return new Promise<boolean>((resolve) => {
      const tone: DialogTone = options.tone ?? 'primary';
      const icon: UiIconName = options.icon ?? this.defaultIconForTone(tone);
      const dialog: ActiveDialog = {
        id: this.nextId++,
        title: options.title,
        message: options.message,
        confirmLabel: options.confirmLabel ?? 'Confirmer',
        cancelLabel: options.cancelLabel ?? 'Annuler',
        tone,
        icon,
        isAlert: false,
        resolve,
      };
      this.activeDialog.set(dialog);
    });
  }

  /** Ouvre une boîte d'alerte / information bloquante et attend l'acquittement de l'utilisateur. */
  alert(options: AlertDialogOptions): Promise<void> {
    return new Promise<void>((resolve) => {
      const tone: DialogTone = options.tone ?? 'info';
      const icon: UiIconName = options.icon ?? this.defaultIconForTone(tone);
      const dialog: ActiveDialog = {
        id: this.nextId++,
        title: options.title,
        message: options.message,
        confirmLabel: options.confirmLabel ?? 'D\'accord',
        tone,
        icon,
        isAlert: true,
        resolve: () => resolve(),
      };
      this.activeDialog.set(dialog);
    });
  }

  /** Ferme le dialogue actif en transmettant la décision de l'utilisateur. */
  close(confirmed: boolean): void {
    const current = this.activeDialog();
    if (!current) return;
    this.activeDialog.set(null);
    current.resolve(confirmed);
  }

  private defaultIconForTone(tone: DialogTone): UiIconName {
    switch (tone) {
      case 'warning':
        return 'exclamation-triangle';
      case 'danger':
        return 'trash';
      case 'primary':
      case 'info':
      default:
        return 'information-circle';
    }
  }
}
