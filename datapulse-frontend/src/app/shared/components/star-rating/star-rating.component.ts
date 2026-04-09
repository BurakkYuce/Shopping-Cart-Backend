import { Component, Input, computed, input } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-star-rating',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="inline-flex items-center" [style.gap.px]="gap">
      <span
        *ngFor="let star of stars(); let i = index"
        class="material-symbols-outlined text-primary"
        [class.filled]="star === 'full' || star === 'half'"
        [style.font-size.px]="size()"
      >{{ star === 'full' ? 'star' : star === 'half' ? 'star_half' : 'star' }}</span>
    </div>
  `,
})
export class StarRatingComponent {
  readonly rating = input<number>(0);
  readonly size = input<number>(16);
  @Input() gap = 2;

  stars = computed(() => {
    const r = this.rating() ?? 0;
    const arr: Array<'full' | 'half' | 'empty'> = [];
    for (let i = 1; i <= 5; i++) {
      if (r >= i) arr.push('full');
      else if (r >= i - 0.5) arr.push('half');
      else arr.push('empty');
    }
    return arr;
  });
}
