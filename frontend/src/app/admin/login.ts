import { Component, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { Auth } from '../core/auth';

@Component({
  selector: 'app-login',
  template: `
    <form class="card reveal" (submit)="submit($event)">
      <h1>Connexion</h1>
      <label for="email">Email</label>
      <input id="email" type="email" autocomplete="username" required [value]="email()" (input)="email.set($any($event.target).value)">
      <label for="pwd">Mot de passe</label>
      <input id="pwd" type="password" [attr.aria-describedby]="error() ? 'login-error' : null" [attr.aria-invalid]="error() ? true : null" autocomplete="current-password" required [value]="password()" (input)="password.set($any($event.target).value)">
      @if (error()) { <p id="login-error" class="error" role="alert">{{ error() }}</p> }
      <p><button class="btn" type="submit" [disabled]="busy()">Se connecter</button></p>
    </form>
  `,
  styles: `form { max-width: 420px; margin: 56px auto; } h1 { margin-top: 0; }`,
})
export class Login {
  private readonly auth = inject(Auth);
  private readonly router = inject(Router);
  protected readonly email = signal('');
  protected readonly password = signal('');
  protected readonly error = signal('');
  protected readonly busy = signal(false);

  protected submit(e: Event) {
    e.preventDefault();
    this.busy.set(true);
    this.error.set('');
    this.auth.login(this.email(), this.password()).subscribe({
      next: () => this.router.navigateByUrl('/admin'),
      error: (err) => {
        this.busy.set(false);
        this.error.set(err.status === 429 ? 'Trop de tentatives, réessayez dans 15 minutes.' : 'Identifiants invalides.');
      },
    });
  }
}
