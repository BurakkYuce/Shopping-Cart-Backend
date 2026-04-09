import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, Validators, AbstractControl, ValidationErrors } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';

import { AuthService } from '../../../core/services/auth.service';
import { NotificationService } from '../../../core/services/notification.service';

function matchPasswords(control: AbstractControl): ValidationErrors | null {
  const pw = control.get('newPassword')?.value;
  const confirm = control.get('confirmPassword')?.value;
  return pw && confirm && pw !== confirm ? { mismatch: true } : null;
}

@Component({
  selector: 'app-reset-password',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  template: `
    <div class="w-full max-w-md">
      <div class="rounded-3xl bg-background-raised p-10 shadow-atelier">
        <div class="mb-8 text-center">
          <h1 class="text-display-sm text-text-primary">Set a new password</h1>
          <p class="mt-2 text-sm text-text-secondary">Choose something strong and memorable.</p>
        </div>
        <form [formGroup]="form" (ngSubmit)="submit()" class="space-y-5">
          <div>
            <label class="label-base">New password</label>
            <input type="password" formControlName="newPassword" placeholder="••••••••" class="input-base" />
          </div>
          <div>
            <label class="label-base">Confirm password</label>
            <input type="password" formControlName="confirmPassword" placeholder="••••••••" class="input-base" />
            <p *ngIf="form.errors?.['mismatch'] && form.touched" class="mt-1.5 text-xs text-danger">Passwords do not match.</p>
          </div>
          <button type="submit" [disabled]="loading() || form.invalid" class="btn-primary w-full">
            <span *ngIf="!loading()">Reset password</span>
            <span *ngIf="loading()" class="material-symbols-outlined animate-spin" style="font-size: 20px">progress_activity</span>
          </button>
        </form>
      </div>
      <p class="mt-6 text-center text-sm text-text-secondary">
        <a routerLink="/auth/login" class="font-semibold text-primary hover:underline">Back to sign in</a>
      </p>
    </div>
  `,
})
export class ResetPasswordComponent {
  private readonly fb = inject(FormBuilder);
  private readonly auth = inject(AuthService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly toast = inject(NotificationService);

  readonly loading = signal<boolean>(false);

  readonly form = this.fb.nonNullable.group(
    {
      newPassword: ['', [Validators.required, Validators.minLength(6)]],
      confirmPassword: ['', [Validators.required]],
    },
    { validators: [matchPasswords] },
  );

  submit(): void {
    if (this.form.invalid) return;
    const token = this.route.snapshot.queryParams['token'] ?? '';
    if (!token) {
      this.toast.error('Missing reset token. Please use the link from your email.');
      return;
    }
    this.loading.set(true);
    this.auth.resetPassword({ token, newPassword: this.form.controls.newPassword.value }).subscribe({
      next: () => {
        this.loading.set(false);
        this.toast.success('Password updated. Please sign in.');
        this.router.navigate(['/auth/login']);
      },
      error: (err) => {
        this.loading.set(false);
        this.toast.error(err?.error?.message ?? 'Reset failed.');
      },
    });
  }
}
