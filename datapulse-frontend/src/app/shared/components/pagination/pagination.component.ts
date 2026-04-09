import { Component, EventEmitter, Output, computed, input } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-pagination',
  standalone: true,
  imports: [CommonModule],
  template: `
    <nav class="flex items-center justify-between gap-4 text-sm" *ngIf="totalPages() > 1">
      <p class="text-text-secondary">
        Page <span class="font-semibold text-text-primary">{{ currentPage() + 1 }}</span> of
        <span class="font-semibold text-text-primary">{{ totalPages() }}</span>
        <span class="ml-2 text-text-tertiary">· {{ totalElements() }} items</span>
      </p>
      <div class="flex items-center gap-2">
        <button
          class="flex h-10 w-10 items-center justify-center rounded-full bg-background-sub text-text-primary transition-all hover:bg-outline disabled:opacity-40"
          [disabled]="currentPage() === 0"
          (click)="go(currentPage() - 1)"
          aria-label="Previous page">
          <span class="material-symbols-outlined" style="font-size: 20px">chevron_left</span>
        </button>
        <ng-container *ngFor="let p of visiblePages()">
          <button *ngIf="p !== -1"
            class="flex h-10 min-w-[2.5rem] items-center justify-center rounded-full px-3 text-sm font-semibold transition-all"
            [class.bg-primary]="p === currentPage()"
            [class.text-white]="p === currentPage()"
            [class.bg-background-sub]="p !== currentPage()"
            [class.text-text-primary]="p !== currentPage()"
            (click)="go(p)">
            {{ p + 1 }}
          </button>
          <span *ngIf="p === -1" class="px-1 text-text-tertiary">…</span>
        </ng-container>
        <button
          class="flex h-10 w-10 items-center justify-center rounded-full bg-background-sub text-text-primary transition-all hover:bg-outline disabled:opacity-40"
          [disabled]="currentPage() >= totalPages() - 1"
          (click)="go(currentPage() + 1)"
          aria-label="Next page">
          <span class="material-symbols-outlined" style="font-size: 20px">chevron_right</span>
        </button>
      </div>
    </nav>
  `,
})
export class PaginationComponent {
  readonly currentPage = input<number>(0);
  readonly totalPages = input<number>(0);
  readonly totalElements = input<number>(0);

  @Output() pageChange = new EventEmitter<number>();

  visiblePages = computed<number[]>(() => {
    const total = this.totalPages();
    const cur = this.currentPage();
    if (total <= 7) return Array.from({ length: total }, (_, i) => i);
    const pages: number[] = [];
    pages.push(0);
    if (cur > 3) pages.push(-1);
    for (let i = Math.max(1, cur - 1); i <= Math.min(total - 2, cur + 1); i++) pages.push(i);
    if (cur < total - 4) pages.push(-1);
    pages.push(total - 1);
    return pages;
  });

  go(page: number): void {
    if (page < 0 || page >= this.totalPages() || page === this.currentPage()) return;
    this.pageChange.emit(page);
  }
}
