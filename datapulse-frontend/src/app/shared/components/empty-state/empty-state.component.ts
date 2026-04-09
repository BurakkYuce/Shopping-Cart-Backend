import { Component, input } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-empty-state',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="flex flex-col items-center justify-center py-20 text-center">
      <div class="mb-4 flex h-20 w-20 items-center justify-center rounded-3xl bg-background-sub">
        <span class="material-symbols-outlined text-text-tertiary" style="font-size: 36px">{{ icon() }}</span>
      </div>
      <h3 class="text-lg font-semibold text-text-primary">{{ title() }}</h3>
      <p *ngIf="message()" class="mt-2 max-w-sm text-sm text-text-secondary">{{ message() }}</p>
      <ng-content></ng-content>
    </div>
  `,
})
export class EmptyStateComponent {
  readonly icon = input<string>('inbox');
  readonly title = input<string>('Nothing here yet');
  readonly message = input<string>('');
}
