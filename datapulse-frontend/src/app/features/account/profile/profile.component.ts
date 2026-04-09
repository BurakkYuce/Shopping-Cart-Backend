import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';

import { UserService } from '../../../core/services/user.service';
import { AuthService } from '../../../core/services/auth.service';
import { NotificationService } from '../../../core/services/notification.service';
import { CustomerProfile, User } from '../../../core/models/user.models';

type ProfileTab = 'personal' | 'security' | 'notifications';

@Component({
  selector: 'app-profile',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './profile.component.html',
})
export class ProfileComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly userService = inject(UserService);
  readonly auth = inject(AuthService);
  private readonly toast = inject(NotificationService);

  readonly tab = signal<ProfileTab>('personal');
  readonly loading = signal<boolean>(false);
  readonly profile = signal<CustomerProfile | null>(null);
  readonly me = signal<User | null>(null);

  readonly personalForm = this.fb.nonNullable.group({
    age: [null as number | null],
    city: [''],
    membershipType: [''],
    gender: [''],
  });

  readonly securityForm = this.fb.nonNullable.group({
    currentPassword: ['', Validators.required],
    newPassword: ['', [Validators.required, Validators.minLength(6)]],
    confirmPassword: ['', Validators.required],
  });

  readonly notifPrefs = signal({
    orderUpdates: true,
    newArrivals: true,
    promotions: false,
    newsletter: true,
  });

  ngOnInit(): void {
    this.userService.getMe().subscribe({
      next: (u) => {
        this.me.set(u);
        this.personalForm.patchValue({ gender: u.gender ?? '' });
      },
      error: () => {},
    });
    this.userService.getMyProfile().subscribe({
      next: (p) => {
        this.profile.set(p);
        this.personalForm.patchValue({
          age: p.age ?? null,
          city: p.city ?? '',
          membershipType: p.membershipType ?? '',
        });
      },
      error: () => {},
    });
  }

  setTab(t: ProfileTab): void {
    this.tab.set(t);
  }

  savePersonal(): void {
    if (this.personalForm.invalid) return;
    this.loading.set(true);
    const value = this.personalForm.getRawValue();
    this.userService
      .updateMyProfile({
        age: value.age ?? undefined,
        city: value.city || undefined,
        membershipType: value.membershipType || undefined,
        gender: value.gender || undefined,
      })
      .subscribe({
        next: (p) => {
          this.profile.set(p);
          this.toast.success('Profile updated.');
          this.loading.set(false);
        },
        error: () => {
          this.toast.error('Could not save profile.');
          this.loading.set(false);
        },
      });
  }

  saveSecurity(): void {
    if (this.securityForm.invalid) return;
    const { currentPassword, newPassword, confirmPassword } = this.securityForm.value;
    if (newPassword !== confirmPassword) {
      this.toast.error('Passwords do not match.');
      return;
    }
    this.loading.set(true);
    this.userService.changePassword(currentPassword!, newPassword!).subscribe({
      next: () => {
        this.toast.success('Password updated successfully.');
        this.securityForm.reset();
        this.loading.set(false);
      },
      error: (err) => {
        const msg = err?.error?.message ?? 'Could not update password.';
        this.toast.error(msg);
        this.loading.set(false);
      },
    });
  }

  togglePref(key: string): void {
    const k = key as keyof ReturnType<typeof this.notifPrefs>;
    this.notifPrefs.update((p) => ({ ...p, [k]: !p[k] }));
  }

  isPrefEnabled(key: string): boolean {
    const k = key as keyof ReturnType<typeof this.notifPrefs>;
    return this.notifPrefs()[k];
  }

  logout(): void {
    this.auth.logout();
  }
}
