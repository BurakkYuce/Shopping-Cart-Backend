import { CommonModule } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './login.html',
  styleUrl: './login.css'
})
export class Login {
  private fb = inject(FormBuilder);
  private authService = inject(AuthService);
  private router = inject(Router);

  isSubmitting = signal(false);
  errorMessage = signal('');

  loginForm = this.fb.nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(6)]]
  });

  onSubmit(): void {
    if (this.loginForm.invalid) {
      this.loginForm.markAllAsTouched();
      return;
    }

    this.isSubmitting.set(true);
    this.errorMessage.set('');

    this.authService.login(this.loginForm.getRawValue()).subscribe({
      next: (response) => {
        this.isSubmitting.set(false);

        if (response.userRole === 'ADMIN') {
          this.router.navigate(['/dashboard/admin']);
          return;
        }

        if (response.userRole === 'CORPORATE') {
          this.router.navigate(['/dashboard/corporate']);
          return;
        }

        this.router.navigate(['/dashboard/me']);
      },
      error: (error: HttpErrorResponse) => {
        this.isSubmitting.set(false);

        if (error.status === 400) {
          this.errorMessage.set('Please check your email and password format.');
          return;
        }

        if (error.status === 404) {
          this.errorMessage.set('User not found.');
          return;
        }

        if (error.error?.message) {
          this.errorMessage.set(error.error.message);
          return;
        }

        this.errorMessage.set('Login failed. Please try again.');
      }
    });
  }

  get email() {
    return this.loginForm.controls.email;
  }

  get password() {
    return this.loginForm.controls.password;
  }
}
