import { Injectable, signal } from '@angular/core';

export type ToastTone = 'success' | 'info' | 'warning' | 'error';

export interface Toast {
  id: number;
  tone: ToastTone;
  message: string;
}

const DEFAULT_DURATION_MS = 5000;

/** Messages de notification et confirmation éphémères, visibles même après une navigation. */
@Injectable({ providedIn: 'root' })
export class ToastService {
  private nextId = 1;
  readonly toasts = signal<Toast[]>([]);

  show(message: string, tone: ToastTone = 'success', durationMs = DEFAULT_DURATION_MS): void {
    const toast: Toast = { id: this.nextId++, tone, message };
    this.toasts.update((items) => [...items, toast]);
    if (durationMs > 0) setTimeout(() => this.dismiss(toast.id), durationMs);
  }

  success(message: string, durationMs = DEFAULT_DURATION_MS): void {
    this.show(message, 'success', durationMs);
  }

  error(message: string, durationMs = DEFAULT_DURATION_MS): void {
    this.show(message, 'error', durationMs);
  }

  warning(message: string, durationMs = DEFAULT_DURATION_MS): void {
    this.show(message, 'warning', durationMs);
  }

  info(message: string, durationMs = DEFAULT_DURATION_MS): void {
    this.show(message, 'info', durationMs);
  }

  dismiss(id: number): void {
    this.toasts.update((items) => items.filter((item) => item.id !== id));
  }
}
