import { Injectable, inject, signal } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { catchError, map, of, tap } from 'rxjs';
import { Api } from './api';

@Injectable({ providedIn: 'root' })
export class Auth {
  private readonly api = inject(Api);
  readonly email = signal<string | null>(null);

  check() {
    return this.api.me().pipe(
      tap((email) => this.email.set(email)),
      map(() => true),
      catchError(() => { this.email.set(null); return of(false); }),
    );
  }

  login(email: string, password: string) {
    return this.api.login(email, password).pipe(tap(() => this.email.set(email)));
  }

  logout() {
    return this.api.logout().pipe(tap(() => this.email.set(null)));
  }
}

export const authGuard: CanActivateFn = () => {
  const router = inject(Router);
  return inject(Auth).check().pipe(map((ok) => ok || router.createUrlTree(['/admin/login'])));
};
