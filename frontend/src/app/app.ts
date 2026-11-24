import { Component, inject, signal } from '@angular/core';
import { NavigationEnd, Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { filter } from 'rxjs';
import { Api, ShabbatStatus } from './core/api';
import { ShabbatClosed } from './pages/shabbat-closed';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, RouterLink, RouterLinkActive, ShabbatClosed],
  template: `
    <div class="stripe"></div>
    <div class="wrap">
      <header class="navbar">
        <a routerLink="/" class="brand">Chemin de Guer <span class="he">גר</span></a>
        <nav>
          <a routerLink="/" routerLinkActive="current" [routerLinkActiveOptions]="{ exact: true }">Articles</a>
          <a href="/rss.xml">RSS</a>
          <a href="https://luah-france.netlify.app/" target="_blank" rel="noopener noreferrer">Luah</a>
        </nav>
      </header>

      <main>
        @if (closed() && !isAdmin()) {
          <app-shabbat-closed [status]="shabbat()!" />
        } @else {
          <router-outlet />
        }
      </main>

      <footer>
        <span>Chemin de Guer · journal d'un parcours de conversion</span>
        @if (shabbat()?.parasha; as p) { <span class="mono">Paracha : {{ p }} {{ shabbat()?.parashaHebrew }}</span> }
      </footer>
    </div>
  `,
  styles: `
    :host { display: block; }
    .stripe { height: 7px; background: repeating-linear-gradient(90deg,
      var(--blue) 0 28px, var(--surface) 28px 34px, var(--blue) 34px 62px,
      var(--surface) 62px 68px, var(--gold) 68px 72px, var(--surface) 72px 78px); }
    .navbar { display: flex; align-items: center; justify-content: space-between; flex-wrap: wrap; gap: 14px;
      margin-top: 22px; padding: 14px 22px; background: var(--surface); border: 1px solid var(--border);
      border-radius: 10px; box-shadow: var(--shadow); }
    .brand { font: 600 1.2rem var(--serif); color: var(--blue-strong); text-decoration: none; }
    .he { color: var(--gold); margin-inline-start: .35rem; }
    nav { display: flex; gap: 22px; font-size: .92rem; }
    nav a { color: var(--ink-muted); text-decoration: none; padding-block: 2px; border-bottom: 2px solid transparent;
      transition: color 160ms ease, border-color 160ms ease; }
    nav a.current { color: var(--blue); font-weight: 600; border-bottom-color: var(--gold); }
    @media (hover: hover) and (pointer: fine) { nav a:hover { color: var(--blue); } }
    main { min-height: 62vh; padding-block: 8px 40px; }
    footer { display: flex; justify-content: space-between; flex-wrap: wrap; gap: 8px; padding-block: 24px 48px;
      border-top: 1px solid var(--border); color: var(--ink-muted); font-size: .88rem; }
  `,
})
export class App {
  private readonly api = inject(Api);
  private readonly router = inject(Router);
  protected readonly shabbat = signal<ShabbatStatus | null>(null);
  protected readonly closed = signal(false);
  protected readonly isAdmin = signal(false);

  constructor() {
    this.refresh();
    this.router.events.pipe(filter((e) => e instanceof NavigationEnd)).subscribe((e) => {
      this.isAdmin.set(e.urlAfterRedirects.startsWith('/admin'));
      this.refresh();
    });
  }

  private refresh() {
    this.api.shabbat().subscribe({
      next: (s) => { this.shabbat.set(s); this.closed.set(s.closed); },
      error: () => this.closed.set(false),
    });
  }
}
