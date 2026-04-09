import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';

import { NotificationService } from '../../../core/services/notification.service';

@Component({
  selector: 'app-toast',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="pointer-events-none fixed bottom-24 right-6 z-[200] flex flex-col gap-2">
      <div
        *ngFor="let t of notifications.toasts()"
        class="pointer-events-auto flex items-center gap-3 rounded-2xl bg-background-raised px-5 py-4 shadow-atelier animate-slide-up"
        [ngClass]="{
          'ring-1 ring-success/30': t.type === 'success',
          'ring-1 ring-danger/30': t.type === 'error',
          'ring-1 ring-warning/30': t.type === 'warning',
          'ring-1 ring-info/30': t.type === 'info'
        }">
        <span class="material-symbols-outlined"
              [class.text-success]="t.type === 'success'"
              [class.text-danger]="t.type === 'error'"
              [class.text-warning]="t.type === 'warning'"
              [class.text-info]="t.type === 'info'"
              style="font-size: 22px">{{
          t.type === 'success' ? 'check_circle' :
          t.type === 'error' ? 'error' :
          t.type === 'warning' ? 'warning' : 'info'
        }}</span>
        <p class="pr-2 text-sm font-medium text-text-primary">{{ t.message }}</p>
        <button (click)="notifications.dismiss(t.id)" class="ml-auto text-text-tertiary transition-colors hover:text-text-primary" aria-label="Dismiss">
          <span class="material-symbols-outlined" style="font-size: 18px">close</span>
        </button>
      </div>
    </div>
  `,
})
export class ToastComponent {
  readonly notifications = inject(NotificationService);
}
