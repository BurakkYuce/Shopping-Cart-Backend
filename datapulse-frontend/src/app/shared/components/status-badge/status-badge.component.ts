import { Component, input, computed } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-status-badge',
  standalone: true,
  imports: [CommonModule],
  template: `
    <span
      class="inline-flex items-center gap-1.5 rounded-full px-3 py-1 text-[11px] font-semibold uppercase tracking-wider"
      [ngClass]="classes()">
      <span class="h-1.5 w-1.5 rounded-full" [ngClass]="dotClasses()"></span>
      {{ label() }}
    </span>
  `,
})
export class StatusBadgeComponent {
  readonly status = input.required<string>();

  label = computed(() => {
    const s = this.status();
    return s.replace(/_/g, ' ');
  });

  classes = computed(() => {
    const s = this.status();
    switch (s) {
      case 'delivered':
      case 'active':
      case 'published':
        return 'bg-success/10 text-success';
      case 'shipped':
      case 'in_transit':
      case 'out_for_delivery':
      case 'processing':
        return 'bg-primary/10 text-primary';
      case 'pending':
        return 'bg-warning/10 text-warning';
      case 'cancelled':
      case 'failed':
      case 'inactive':
        return 'bg-danger/10 text-danger';
      case 'return_requested':
      case 'returned':
      case 'draft':
      default:
        return 'bg-background-sub text-text-secondary';
    }
  });

  dotClasses = computed(() => {
    const s = this.status();
    switch (s) {
      case 'delivered':
      case 'active':
      case 'published':
        return 'bg-success';
      case 'shipped':
      case 'in_transit':
      case 'out_for_delivery':
      case 'processing':
        return 'bg-primary';
      case 'pending':
        return 'bg-warning';
      case 'cancelled':
      case 'failed':
      case 'inactive':
        return 'bg-danger';
      default:
        return 'bg-text-tertiary';
    }
  });
}
