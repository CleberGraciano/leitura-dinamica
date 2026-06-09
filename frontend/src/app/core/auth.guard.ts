import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';

import { AuthSessionService } from './auth-session.service';

export const authGuard: CanActivateFn = () => {
  const authSession = inject(AuthSessionService);
  const router = inject(Router);
  return authSession.isAuthenticated() ? true : router.createUrlTree(['/auth']);
};

export const guestGuard: CanActivateFn = () => {
  const authSession = inject(AuthSessionService);
  const router = inject(Router);
  return authSession.isAuthenticated() ? router.createUrlTree(['/app/dashboard']) : true;
};

export const roleGuard = (allowedRoles: Array<'ROLE_USER' | 'ROLE_ADMIN' | 'ROLE_EDITOR'>): CanActivateFn => () => {
  const authSession = inject(AuthSessionService);
  const router = inject(Router);
  const currentUser = authSession.currentUser();

  if (!currentUser) {
    return router.createUrlTree(['/auth']);
  }

  return allowedRoles.includes(currentUser.role) ? true : router.createUrlTree(['/app/dashboard']);
};

export const adminGuard = roleGuard(['ROLE_ADMIN', 'ROLE_EDITOR']);