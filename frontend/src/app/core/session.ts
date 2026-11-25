import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';

/**
 * Session expirée : les sessions vivent en mémoire côté serveur, donc un redémarrage du backend (déploiement)
 * les perd. Plutôt que d'afficher une administration vide, on renvoie vers la connexion.
 */
export const sessionInterceptor: HttpInterceptorFn = (req, next) => {
  const router = inject(Router);
  return next(req).pipe(
    catchError((err: unknown) => {
      if (err instanceof HttpErrorResponse && err.status === 401 && req.url.startsWith('/api/') && !req.url.startsWith('/api/auth/')) {
        router.navigateByUrl('/admin/login');
      }
      return throwError(() => err);
    }),
  );
};
