import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

import { CouponService } from '../../../core/services/coupon.service';
import { Coupon, CreateCouponRequest } from '../../../core/models/coupon.models';
import { NotificationService } from '../../../core/services/notification.service';
import { SpinnerComponent } from '../../../shared/components/spinner/spinner.component';

@Component({
  selector: 'app-admin-coupons',
  standalone: true,
  imports: [CommonModule, FormsModule, SpinnerComponent],
  template: `
    <header class="sticky top-0 z-40 flex items-center justify-between border-b border-outline/40 bg-background/90 px-12 py-6 backdrop-blur-xl">
      <div class="flex items-center gap-8">
        <span class="text-2xl font-bold tracking-tight text-text-primary">DataPulse</span>
        <h2 class="border-l border-outline/40 pl-8 text-xl font-semibold text-text-primary">Coupon Management</h2>
      </div>
      <button (click)="openForm()" class="btn-primary h-10 gap-2">
        <span class="material-symbols-outlined" style="font-size: 18px">add</span>
        New Coupon
      </button>
    </header>

    <section class="px-12 py-10">
      <app-spinner *ngIf="loading()"></app-spinner>

      <!-- Create / Edit Form -->
      <div *ngIf="showForm()" class="mb-8 rounded-3xl bg-background-raised p-8 shadow-atelier-sm">
        <h3 class="text-sm font-bold uppercase tracking-wider text-text-primary">
          {{ editing() ? 'Edit Coupon' : 'Create New Coupon' }}
        </h3>
        <div class="mt-6 grid gap-5 sm:grid-cols-2 lg:grid-cols-3">
          <div>
            <label class="label-base">Code</label>
            <input type="text" [(ngModel)]="form.code" class="input-base bg-background-sub" placeholder="WELCOME10"
                   [disabled]="!!editing()" />
          </div>
          <div>
            <label class="label-base">Type</label>
            <select [(ngModel)]="form.type" class="input-base bg-background-sub">
              <option value="PERCENTAGE">Percentage</option>
              <option value="FIXED_AMOUNT">Fixed Amount</option>
            </select>
          </div>
          <div>
            <label class="label-base">Value</label>
            <input type="number" [(ngModel)]="form.value" class="input-base bg-background-sub" placeholder="10" />
          </div>
          <div class="sm:col-span-2 lg:col-span-3">
            <label class="label-base">Description</label>
            <input type="text" [(ngModel)]="form.description" class="input-base bg-background-sub" placeholder="10% off your first order" />
          </div>
          <div>
            <label class="label-base">Min Order Amount</label>
            <input type="number" [(ngModel)]="form.minOrderAmount" class="input-base bg-background-sub" placeholder="0" />
          </div>
          <div>
            <label class="label-base">Max Discount</label>
            <input type="number" [(ngModel)]="form.maxDiscount" class="input-base bg-background-sub" placeholder="No limit" />
          </div>
          <div>
            <label class="label-base">Max Uses</label>
            <input type="number" [(ngModel)]="form.maxUses" class="input-base bg-background-sub" placeholder="Unlimited" />
          </div>
          <div>
            <label class="label-base">Valid From</label>
            <input type="datetime-local" [(ngModel)]="form.validFrom" class="input-base bg-background-sub" />
          </div>
          <div>
            <label class="label-base">Valid To</label>
            <input type="datetime-local" [(ngModel)]="form.validTo" class="input-base bg-background-sub" />
          </div>
        </div>
        <div class="mt-6 flex items-center gap-3">
          <button (click)="save()" [disabled]="saving()" class="btn-primary h-10">
            {{ saving() ? 'Saving...' : editing() ? 'Update' : 'Create' }}
          </button>
          <button (click)="closeForm()" class="btn-secondary h-10">Cancel</button>
        </div>
      </div>

      <!-- Coupons Table -->
      <div *ngIf="!loading()" class="overflow-x-auto rounded-3xl bg-background-raised shadow-atelier-sm">
        <table class="w-full text-sm">
          <thead>
            <tr class="border-b border-outline/40 text-left text-xs font-bold uppercase tracking-wider text-text-tertiary">
              <th class="px-6 py-4">Code</th>
              <th class="px-4 py-4">Type</th>
              <th class="px-4 py-4 text-right">Value</th>
              <th class="px-4 py-4">Description</th>
              <th class="px-4 py-4 text-right">Min Order</th>
              <th class="px-4 py-4 text-right">Max Discount</th>
              <th class="px-4 py-4 text-center">Uses</th>
              <th class="px-4 py-4">Valid Period</th>
              <th class="px-4 py-4 text-center">Status</th>
              <th class="px-6 py-4 text-right">Actions</th>
            </tr>
          </thead>
          <tbody>
            <tr *ngFor="let c of coupons()" class="border-b border-outline/20 hover:bg-background-sub/50">
              <td class="px-6 py-4">
                <code class="rounded-md bg-primary/10 px-2 py-1 text-xs font-bold text-primary">{{ c.code }}</code>
              </td>
              <td class="px-4 py-4 text-text-secondary">
                {{ c.type === 'PERCENTAGE' ? 'Percentage' : 'Fixed' }}
              </td>
              <td class="px-4 py-4 text-right font-semibold text-text-primary">
                {{ c.type === 'PERCENTAGE' ? c.value + '%' : '$' + c.value }}
              </td>
              <td class="max-w-[200px] truncate px-4 py-4 text-text-secondary">{{ c.description }}</td>
              <td class="px-4 py-4 text-right text-text-secondary">
                {{ c.minOrderAmount ? ('$' + c.minOrderAmount) : '-' }}
              </td>
              <td class="px-4 py-4 text-right text-text-secondary">
                {{ c.maxDiscount ? ('$' + c.maxDiscount) : '-' }}
              </td>
              <td class="px-4 py-4 text-center text-text-secondary">
                {{ c.currentUses }}/{{ c.maxUses ?? '∞' }}
              </td>
              <td class="px-4 py-4 text-xs text-text-tertiary">
                <div *ngIf="c.validFrom">{{ c.validFrom | date:'MMM d, y' }}</div>
                <div *ngIf="c.validTo">to {{ c.validTo | date:'MMM d, y' }}</div>
                <span *ngIf="!c.validFrom && !c.validTo">No limit</span>
              </td>
              <td class="px-4 py-4 text-center">
                <span class="rounded-full px-3 py-1 text-[10px] font-black uppercase"
                      [class.bg-success/10]="c.active" [class.text-success]="c.active"
                      [class.bg-danger/10]="!c.active" [class.text-danger]="!c.active">
                  {{ c.active ? 'Active' : 'Inactive' }}
                </span>
              </td>
              <td class="px-6 py-4 text-right">
                <div class="flex items-center justify-end gap-1">
                  <button (click)="edit(c)" class="flex h-8 w-8 items-center justify-center rounded-lg text-text-tertiary hover:bg-background-sub hover:text-primary"
                          title="Edit">
                    <span class="material-symbols-outlined" style="font-size: 18px">edit</span>
                  </button>
                  <button *ngIf="c.active" (click)="deactivate(c)" class="flex h-8 w-8 items-center justify-center rounded-lg text-text-tertiary hover:bg-danger/10 hover:text-danger"
                          title="Deactivate">
                    <span class="material-symbols-outlined" style="font-size: 18px">block</span>
                  </button>
                </div>
              </td>
            </tr>
            <tr *ngIf="coupons().length === 0">
              <td colspan="10" class="px-6 py-16 text-center text-text-tertiary">
                No coupons yet. Create one to get started.
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </section>
  `,
})
export class AdminCouponsComponent implements OnInit {
  private readonly couponService = inject(CouponService);
  private readonly toast = inject(NotificationService);

  readonly coupons = signal<Coupon[]>([]);
  readonly loading = signal<boolean>(true);
  readonly showForm = signal<boolean>(false);
  readonly editing = signal<Coupon | null>(null);
  readonly saving = signal<boolean>(false);

  form: CreateCouponRequest & { minOrderAmount?: number; maxDiscount?: number; maxUses?: number; validFrom?: string; validTo?: string } = {
    code: '',
    type: 'PERCENTAGE',
    value: 0,
    description: '',
    minOrderAmount: undefined,
    maxDiscount: undefined,
    maxUses: undefined,
    validFrom: undefined,
    validTo: undefined,
  };

  ngOnInit(): void {
    this.fetch();
  }

  fetch(): void {
    this.loading.set(true);
    this.couponService.listCoupons().subscribe({
      next: (coupons) => {
        this.coupons.set(coupons);
        this.loading.set(false);
      },
      error: () => {
        this.coupons.set([]);
        this.loading.set(false);
        this.toast.error('Could not load coupons.');
      },
    });
  }

  openForm(): void {
    this.editing.set(null);
    this.form = { code: '', type: 'PERCENTAGE', value: 0, description: '' };
    this.showForm.set(true);
  }

  closeForm(): void {
    this.showForm.set(false);
    this.editing.set(null);
  }

  edit(c: Coupon): void {
    this.editing.set(c);
    this.form = {
      code: c.code,
      type: c.type,
      value: c.value,
      description: c.description,
      minOrderAmount: c.minOrderAmount || undefined,
      maxDiscount: c.maxDiscount || undefined,
      maxUses: c.maxUses || undefined,
      validFrom: c.validFrom ? c.validFrom.substring(0, 16) : undefined,
      validTo: c.validTo ? c.validTo.substring(0, 16) : undefined,
    };
    this.showForm.set(true);
  }

  save(): void {
    if (!this.form.code || !this.form.value) {
      this.toast.error('Code and value are required.');
      return;
    }
    this.saving.set(true);
    const existing = this.editing();

    if (existing) {
      this.couponService.updateCoupon(existing.id, this.form).subscribe({
        next: () => {
          this.toast.success('Coupon updated.');
          this.saving.set(false);
          this.closeForm();
          this.fetch();
        },
        error: (err) => {
          this.toast.error(err?.error?.message ?? 'Could not update coupon.');
          this.saving.set(false);
        },
      });
    } else {
      this.couponService.createCoupon(this.form).subscribe({
        next: () => {
          this.toast.success('Coupon created.');
          this.saving.set(false);
          this.closeForm();
          this.fetch();
        },
        error: (err) => {
          this.toast.error(err?.error?.message ?? 'Could not create coupon.');
          this.saving.set(false);
        },
      });
    }
  }

  deactivate(c: Coupon): void {
    this.couponService.deactivateCoupon(c.id).subscribe({
      next: () => {
        this.toast.success(`Coupon "${c.code}" deactivated.`);
        this.fetch();
      },
      error: () => this.toast.error('Could not deactivate coupon.'),
    });
  }
}
