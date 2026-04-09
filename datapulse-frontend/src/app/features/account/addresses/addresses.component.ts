import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';

import { AddressService } from '../../../core/services/address.service';
import { Address } from '../../../core/models/user.models';
import { NotificationService } from '../../../core/services/notification.service';
import { SpinnerComponent } from '../../../shared/components/spinner/spinner.component';
import { EmptyStateComponent } from '../../../shared/components/empty-state/empty-state.component';

@Component({
  selector: 'app-addresses',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, SpinnerComponent, EmptyStateComponent],
  templateUrl: './addresses.component.html',
})
export class AddressesComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly addressService = inject(AddressService);
  private readonly toast = inject(NotificationService);

  readonly addresses = signal<Address[]>([]);
  readonly loading = signal<boolean>(true);
  readonly showForm = signal<boolean>(false);
  readonly editingId = signal<string | null>(null);
  readonly saving = signal<boolean>(false);

  readonly form = this.fb.nonNullable.group({
    title: ['', Validators.required],
    fullName: ['', Validators.required],
    phone: [''],
    addressLine1: ['', Validators.required],
    addressLine2: [''],
    city: ['', Validators.required],
    district: [''],
    zipCode: [''],
    country: ['Türkiye', Validators.required],
    isDefault: [false],
  });

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.addressService.list().subscribe({
      next: (list) => {
        this.addresses.set(list);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }

  openNew(): void {
    this.editingId.set(null);
    this.form.reset({ country: 'Türkiye', isDefault: false });
    this.showForm.set(true);
  }

  edit(addr: Address): void {
    this.editingId.set(addr.id);
    this.form.patchValue(addr);
    this.showForm.set(true);
  }

  cancel(): void {
    this.showForm.set(false);
    this.editingId.set(null);
  }

  save(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.saving.set(true);
    const payload = this.form.getRawValue();
    const id = this.editingId();
    const obs = id ? this.addressService.update(id, payload) : this.addressService.create(payload);
    obs.subscribe({
      next: () => {
        this.toast.success(id ? 'Address updated.' : 'Address saved.');
        this.saving.set(false);
        this.showForm.set(false);
        this.editingId.set(null);
        this.load();
      },
      error: () => {
        this.saving.set(false);
        this.toast.error('Could not save address.');
      },
    });
  }

  remove(addr: Address): void {
    if (!confirm(`Delete "${addr.title}"?`)) return;
    this.addressService.delete(addr.id).subscribe({
      next: () => {
        this.toast.success('Address deleted.');
        this.load();
      },
      error: () => this.toast.error('Could not delete address.'),
    });
  }

  setDefault(addr: Address): void {
    // Backend doesn't expose a standalone setDefault endpoint — re-submit the
    // address via update with isDefault: true. Other addresses are cleared by
    // the backend's default-uniqueness constraint.
    this.addressService.update(addr.id, { ...addr, isDefault: true }).subscribe({
      next: () => {
        this.toast.success('Default address updated.');
        this.load();
      },
      error: () => this.toast.error('Could not set default.'),
    });
  }
}
