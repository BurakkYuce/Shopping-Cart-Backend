import { CommonModule } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { AuthService } from '../../../core/services/auth.service';
import { UserRole } from '../../../core/models/auth.model';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './register.html',
  styleUrl: './register.css',
})
export class Register {
  private fb = inject(FormBuilder);
  private authService = inject(AuthService);
  private router = inject(Router);

  isSubmitting = signal(false);
  errorMessage = signal('');

  roles: UserRole[] = ['INDIVIDUAL', 'CORPORATE'];

  registerForm = this.fb.nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(6)]],
    roleType: ['INDIVIDUAL' as UserRole, [Validators.required]],
    gender: [''],
  });

  onSubmit(): void {
    if (this.registerForm.invalid) {
      this.registerForm.markAllAsTouched();
      return;
    }

    this.isSubmitting.set(true);
    this.errorMessage.set('');

    const formValue = this.registerForm.getRawValue();

    this.authService
      .register({
        email: formValue.email,
        password: formValue.password,
        roleType: formValue.roleType,
        gender: formValue.gender || undefined,
      })
      .subscribe({
        next: (response) => {
          this.isSubmitting.set(false);

          if (response.userRole === 'CORPORATE') {
            this.router.navigate(['/dashboard/corporate']);
            return;
          }

          this.router.navigate(['/dashboard/me']);
        },
        error: (error: HttpErrorResponse) => {
          this.isSubmitting.set(false);

          console.error('REGISTER ERROR FULL:', error);
          console.error('REGISTER ERROR BODY:', error.error);

          if (error.status === 400) {
            if (error.error?.fieldErrors) {
              const fieldErrors = Object.values(error.error.fieldErrors).join(' | ');
              this.errorMessage.set(fieldErrors);
              return;
            }

            this.errorMessage.set(error.error?.message || 'Please fill in the form correctly.');
            return;
          }

          if (error.status === 409) {
            this.errorMessage.set('This email is already registered.');
            return;
          }

          if (error.error?.message) {
            this.errorMessage.set(error.error.message);
            return;
          }

          this.errorMessage.set(`Registration failed. Status: ${error.status}`);
        },
      });
  }

  get email() {
    return this.registerForm.controls.email;
  }

  get password() {
    return this.registerForm.controls.password;
  }

  get roleType() {
    return this.registerForm.controls.roleType;
  }
}
