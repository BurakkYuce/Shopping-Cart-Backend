import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { Router } from '@angular/router';

import { CartService } from '../../core/services/cart.service';
import { AddressService } from '../../core/services/address.service';
import { NotificationService } from '../../core/services/notification.service';
import { Address } from '../../core/models/user.models';
import { PaymentMethod } from '../../core/models/order.models';

type CheckoutStep = 1 | 2 | 3 | 4;

@Component({
  selector: 'app-checkout',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './checkout.component.html',
})
export class CheckoutComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  readonly cart = inject(CartService);
  private readonly addressService = inject(AddressService);
  private readonly toast = inject(NotificationService);
  private readonly router = inject(Router);

  readonly step = signal<CheckoutStep>(1);
  readonly loading = signal<boolean>(false);
  readonly addresses = signal<Address[]>([]);
  readonly selectedAddressId = signal<string | null>(null);
  readonly selectedBillingId = signal<string | null>(null);
  readonly sameAsShipping = signal<boolean>(true);
  readonly paymentMethod = signal<PaymentMethod>('credit_card');
  readonly showNewAddressForm = signal<boolean>(false);
  readonly notes = signal<string>('');

  readonly addressForm = this.fb.nonNullable.group({
    title: ['', Validators.required],
    fullName: ['', Validators.required],
    phone: [''],
    addressLine1: ['', Validators.required],
    addressLine2: [''],
    city: ['', Validators.required],
    district: [''],
    zipCode: [''],
    country: ['Turkey', Validators.required],
    isDefault: [false],
  });

  readonly cardForm = this.fb.nonNullable.group({
    cardNumber: ['', [Validators.required, Validators.pattern(/^[\d\s]{13,19}$/)]],
    cardName: ['', Validators.required],
    expiry: ['', [Validators.required, Validators.pattern(/^\d{2}\/\d{2}$/)]],
    cvv: ['', [Validators.required, Validators.pattern(/^\d{3,4}$/)]],
  });

  readonly subtotal = computed(() => this.cart.cartSig().totalPrice);
  readonly appliedCouponCode = computed(() => this.cart.appliedCouponCode());
  readonly shipping = computed(() => {
    const items = this.cart.cartSig().items;
    const storeSubtotals = new Map<string, number>();
    for (const item of items) {
      const key = item.storeId || 'unknown';
      storeSubtotals.set(key, (storeSubtotals.get(key) || 0) + item.lineTotal);
    }
    let total = 0;
    storeSubtotals.forEach((sub) => { if (sub <= 100) total += 9.9; });
    return total;
  });
  readonly tax = computed(() => +(this.subtotal() * 0.08).toFixed(2));
  readonly total = computed(() => +(this.subtotal() + this.shipping() + this.tax()).toFixed(2));

  ngOnInit(): void {
    this.cart.fetch().subscribe({
      next: (cart) => {
        if (cart.items.length === 0) this.router.navigate(['/cart']);
      },
    });
    this.loadAddresses();
  }

  loadAddresses(): void {
    this.addressService.list().subscribe({
      next: (list) => {
        this.addresses.set(list);
        const def = list.find((a) => a.isDefault) ?? list[0];
        if (def) {
          this.selectedAddressId.set(def.id);
          this.selectedBillingId.set(def.id);
        }
        if (list.length === 0) this.showNewAddressForm.set(true);
      },
    });
  }

  selectAddress(id: string): void {
    this.selectedAddressId.set(id);
    if (this.sameAsShipping()) this.selectedBillingId.set(id);
  }

  selectBilling(id: string): void {
    this.selectedBillingId.set(id);
  }

  toggleSameAsShipping(): void {
    const v = !this.sameAsShipping();
    this.sameAsShipping.set(v);
    if (v) this.selectedBillingId.set(this.selectedAddressId());
  }

  toggleNewAddress(): void {
    this.showNewAddressForm.update((v) => !v);
  }

  saveNewAddress(): void {
    if (this.addressForm.invalid) {
      this.addressForm.markAllAsTouched();
      return;
    }
    this.loading.set(true);
    this.addressService.create(this.addressForm.getRawValue()).subscribe({
      next: (addr) => {
        const updated = [...this.addresses(), addr];
        this.addresses.set(updated);
        this.selectAddress(addr.id);
        this.showNewAddressForm.set(false);
        this.addressForm.reset({ country: 'Turkey', isDefault: false });
        this.toast.success('Address saved.');
        this.loading.set(false);
      },
      error: () => {
        this.toast.error('Could not save address.');
        this.loading.set(false);
      },
    });
  }

  setPayment(m: PaymentMethod): void {
    this.paymentMethod.set(m);
  }

  nextStep(): void {
    if (this.step() === 1 && !this.selectedAddressId()) {
      this.toast.info('Please select or add a shipping address.');
      return;
    }
    if (this.step() === 2 && this.paymentMethod() === 'credit_card' && this.cardForm.invalid) {
      this.cardForm.markAllAsTouched();
      this.toast.info('Please complete the payment details.');
      return;
    }
    if (this.step() < 4) {
      this.step.update((s) => (s + 1) as CheckoutStep);
    }
  }

  prevStep(): void {
    if (this.step() > 1) this.step.update((s) => (s - 1) as CheckoutStep);
  }

  placeOrder(): void {
    const shipId = this.selectedAddressId();
    if (!shipId) return;

    const cart = this.cart.cartSig();
    if (!cart.items.length) {
      this.toast.info('Your bag is empty.');
      return;
    }

    this.loading.set(true);

    this.cart.checkout(this.paymentMethod()).subscribe({
      next: (orders) => {
        this.toast.success(`${orders.length} order${orders.length > 1 ? 's' : ''} placed successfully.`);
        this.router.navigate(['/orders', orders[0].id, 'confirmation']);
      },
      error: (err) => {
        this.loading.set(false);
        this.toast.error(err?.error?.message ?? 'Could not place order.');
      },
    });
  }
}
