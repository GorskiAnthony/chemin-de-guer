import { DatePipe } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Api, Fete } from '../core/api';
import { Seo } from '../core/seo';

@Component({
  selector: 'app-fete-list',
  imports: [RouterLink, DatePipe],
  template: `
    <section class="hero">
      <p class="caption">Le calendrier juif</p>
      <h1>Fêtes</h1>
      <p class="sub">L'histoire, les coutumes et ce que j'ai appris de chaque fête, jeûne et jour particulier de l'année.</p>
    </section>

    @for (g of groups(); track g.category) {
      <h2 class="cat">{{ g.category }}</h2>
      <div class="grid">
        @for (f of g.items; track f.id) {
          <a class="card item reveal" [routerLink]="['/fetes', f.slug]">
            <span class="top">
              <span class="name">{{ f.name }}</span>
              @if (f.hebrewName) { <span class="he" lang="he" dir="rtl">{{ f.hebrewName }}</span> }
            </span>
            <span class="mono">Rédigé le <time [attr.datetime]="f.createdAt">{{ f.createdAt | date: 'd MMM y' : undefined : 'fr' }}</time></span>
          </a>
        }
      </div>
    } @empty {
      @if (!loading()) { <p class="muted empty">Aucun résumé pour le moment.</p> }
    }
  `,
  styles: `
    .hero { text-align: center; padding: 44px 0 28px; }
    h1 { font-size: clamp(2.2rem, 6vw, 3.4rem); margin: 8px 0 0; }
    .sub { max-width: 52ch; margin: 14px auto 0; color: var(--ink-muted); font-size: 1.05rem; }
    .cat { font-size: 1.5rem; margin: 2.2rem 0 14px; padding-bottom: 8px; border-bottom: 1px solid var(--border); }
    .grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(200px, 1fr)); gap: 14px; }
    .item { display: flex; flex-direction: column; gap: 6px; text-decoration: none;
      color: var(--ink); padding: 16px 18px; border-left: 3px solid var(--gold);
      transition: transform 200ms var(--ease-out), border-color 200ms ease; }
    .item:active { transform: scale(0.98); }
    @media (hover: hover) and (pointer: fine) { .item:hover { transform: translateY(-2px); border-color: var(--blue); } }
    .top { display: flex; justify-content: space-between; align-items: baseline; gap: 10px; }
    .name { font: 600 1.15rem var(--serif); color: var(--blue-strong); }
    .he { color: var(--gold-ink); font-family: var(--serif); }
    .empty { text-align: center; padding: 40px 0; }
  `,
})
export class FeteList {
  private readonly api = inject(Api);
  protected readonly items = signal<Fete[]>([]);
  protected readonly loading = signal(true);
  protected readonly groups = computed(() => {
    const map = new Map<string, Fete[]>();
    for (const f of this.items()) map.set(f.category, [...(map.get(f.category) ?? []), f]);
    return [...map].map(([category, items]) => ({ category, items }));
  });

  constructor() {
    inject(Seo).set({
      title: 'Fêtes · Chemin de Guer',
      description: "Mes résumés des fêtes juives : histoire, coutumes, prières et ce que j'en ai appris.",
      path: '/fetes',
    });
    this.api.publicFetes().subscribe({
      next: (f) => { this.items.set(f); this.loading.set(false); },
      error: () => this.loading.set(false),
    });
  }
}
