import { Component, inject, signal } from '@angular/core';
import { NavigationEnd, Router, RouterLink, RouterOutlet } from '@angular/router';
import { Api, ShabbatStatus } from './core/api';
import { ShabbatClosed } from './pages/shabbat-closed';
import { filter } from 'rxjs';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, RouterLink, ShabbatClosed],
  template: `
    <header class="top">
      <div class="container bar">
        <a routerLink="/" class="brand">Chemin de Guer <span class="he">גר</span></a>
        <nav>
          <a routerLink="/">Articles</a>
          <a href="/rss.xml">RSS</a>
          <a href="https://luah-france.netlify.app/" target="_blank" rel="noopener noreferrer">Luah</a>
        </nav>
      </div>
    </header>
    <main class="container">
      @if (closed() && !isAdmin()) {
        <app-shabbat-closed [status]="shabbat()!" />
      } @else {
        <router-outlet />
      }
    </main>
    <footer class="container muted foot">
      Chemin de Guer · Journal d'un parcours de conversion
      @if (shabbat()?.parasha; as p) { · Paracha de la semaine : {{ p }} }
    </footer>
  `,
  styles: `
    .top { background: var(--blue-strong); color: #fff; border-bottom: 4px solid var(--gold); }
    .bar { display: flex; justify-content: space-between; align-items: center; padding-block: 1rem; flex-wrap: wrap; gap: .5rem; }
    .brand { font: 700 1.5rem "Frank Ruhl Libre", serif; color: #fff; text-decoration: none; }
    .he { color: var(--gold-soft); margin-inline-start: .3rem; }
    nav a { color: #fff; margin-inline-start: 1.2rem; text-decoration: none; font-weight: 500; }
    nav a:hover { color: var(--gold-soft); }
    main { padding-block: 2rem; min-height: 70vh; }
    .foot { padding-block: 1.5rem 2.5rem; font-size: .9rem; }
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
