import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';

import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-forgot-password',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  template: `
    <div class="w-full max-w-md">
      <div class="rounded-3xl bg-background-raised p-10 shadow-atelier">
        <div class="mb-8 text-center">
          <h1 class="text-display-sm text-text-primary">Forgot password?</h1>
          <p class="mt-2 text-sm text-text-secondary">We'll send a reset link to your email.</p>
        </div>

        <div *ngIf="sent()" class="mb-6 rounded-xl bg-success/10 p-4 text-sm text-success">
          Check your inbox. If an account exists, you'll receive a reset link shortly.
        </div>

        <form [formGroup]="form" (ngSubmit)="submit()" class="space-y-5">
          <div>
            <label class="label-base">Email</label>
            <input type="email" formControlName="email" placeholder="you@example.com" class="input-base" />
          </div>
          <button type="submit" [disabled]="loading()" class="btn-primary w-full">
            <span *ngIf="!loading()">Send reset link</span>
            <span *ngIf="loading()" class="material-symbols-outlined animate-spin" style="font-size: 20px">progress_activity</span>
          </button>
        </form>
      </div>
      <p class="mt-6 text-center text-sm text-text-secondary">
        Remembered it?
        <a routerLink="/auth/login" class="font-semibold text-primary hover:underline">Sign in</a>
      </p>
    </div>
  `,
})
export class ForgotPasswordComponent {
  private readonly fb = inject(FormBuilder);
  private readonly auth = inject(AuthService);

  readonly loading = signal<boolean>(false);
  readonly sent = signal<boolean>(false);

  readonly form = this.fb.nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
  });

  submit(): void {
    if (this.form.invalid) return;
    this.loading.set(true);
    this.auth.forgotPassword(this.form.getRawValue()).subscribe({
      next: () => {
        this.loading.set(false);
        this.sent.set(true);
      },
      error: () => {
        this.loading.set(false);
        this.sent.set(true); // Reveal nothing about account existence
      },
    });
  }
}
