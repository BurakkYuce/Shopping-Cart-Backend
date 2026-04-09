import { CanActivateFn, Router } from '@angular/router';
import { inject } from '@angular/core';

import { AuthService } from '../services/auth.service';
import { UserRole } from '../models/auth.models';

/**
 * Role-based route guard.
 *
 *   CORPORATE → /seller/** only
 *   ADMIN     → /admin/**  only
 *   unauthenticated → /auth/login
 *
 * Usage: `canActivate: [roleGuard(['ADMIN'])]`
 */
export function roleGuard(allowedRoles: UserRole[]): CanActivateFn {
  return (route, state) => {
    const authService = inject(AuthService);
    const router = inject(Router);

    if (!authService.isAuthenticated()) {
      return router.createUrlTree(['/auth/login'], {
        queryParams: { returnUrl: state.url },
      });
    }

    const userRole = authService.getUserRole();
    if (userRole && allowedRoles.includes(userRole)) {
      return true;
    }

    // Already logged in with wrong role — bounce to their home.
    if (userRole === 'ADMIN') return router.createUrlTree(['/admin']);
    if (userRole === 'CORPORATE') return router.createUrlTree(['/seller']);
    return router.createUrlTree(['/']);
  };
}

export const guestGuard: CanActivateFn = (route, state) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  if (!authService.isAuthenticated()) return true;

  const role = authService.getUserRole();
  if (role === 'ADMIN') return router.createUrlTree(['/admin']);
  if (role === 'CORPORATE') return router.createUrlTree(['/seller']);
  return router.createUrlTree(['/']);
};
