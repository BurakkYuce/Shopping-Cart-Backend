import { Injectable, signal, computed } from '@angular/core';

export type ToastType = 'success' | 'error' | 'info' | 'warning';

export interface Toast {
  id: number;
  type: ToastType;
  message: string;
  duration?: number;
}

@Injectable({ providedIn: 'root' })
export class NotificationService {
  private readonly _toasts = signal<Toast[]>([]);
  readonly toasts = this._toasts.asReadonly();
  private nextId = 0;

  success(message: string, duration = 3500): void {
    this.push({ type: 'success', message, duration });
  }

  error(message: string, duration = 5000): void {
    this.push({ type: 'error', message, duration });
  }

  info(message: string, duration = 3500): void {
    this.push({ type: 'info', message, duration });
  }

  warning(message: string, duration = 4000): void {
    this.push({ type: 'warning', message, duration });
  }

  dismiss(id: number): void {
    this._toasts.update((ts) => ts.filter((t) => t.id !== id));
  }

  private push(partial: Omit<Toast, 'id'>): void {
    const toast: Toast = { id: ++this.nextId, ...partial };
    this._toasts.update((ts) => [...ts, toast]);
    if (toast.duration) {
      setTimeout(() => this.dismiss(toast.id), toast.duration);
    }
  }
}
