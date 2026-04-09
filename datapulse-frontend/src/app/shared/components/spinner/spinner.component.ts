import { Component, input } from '@angular/core';

@Component({
  selector: 'app-spinner',
  standalone: true,
  template: `
    <div class="flex items-center justify-center py-16">
      <span class="material-symbols-outlined animate-spin text-primary" [style.font-size.px]="size()">progress_activity</span>
    </div>
  `,
})
export class SpinnerComponent {
  readonly size = input<number>(32);
}
