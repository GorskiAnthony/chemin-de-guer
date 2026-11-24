import { Component, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Api, Parasha } from '../core/api';

@Component({
  selector: 'app-parasha-list',
  imports: [RouterLink],
  template: `
    <section class="hero">
      <p class="caption">Étude hebdomadaire</p>
      <h1>Paracha</h1>
      <p class="sub">Ce que j'ai appris, lu et compris de chaque paracha, montée après montée.</p>
    </section>

    @for (g of groups(); track g.book) {
      <h2 class="book">{{ g.book }}</h2>
      <div class="grid">
        @for (p of g.items; track p.id) {
          <a class="card item reveal" [routerLink]="['/paracha', p.slug]">
            <span class="name">{{ p.name }}</span>
            @if (p.hebrewName) { <span class="he" lang="he" dir="rtl">{{ p.hebrewName }}</span> }
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
    .book { font-size: 1.5rem; margin: 2.2rem 0 14px; padding-bottom: 8px; border-bottom: 1px solid var(--border); }
    .grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(200px, 1fr)); gap: 14px; }
    .item { display: flex; justify-content: space-between; align-items: baseline; gap: 10px; text-decoration: none;
      color: var(--ink); padding: 16px 18px; border-left: 3px solid var(--gold);
      transition: transform 200ms var(--ease-out), border-color 200ms ease; }
    .item:active { transform: scale(0.98); }
    @media (hover: hover) and (pointer: fine) { .item:hover { transform: translateY(-2px); border-color: var(--blue); } }
    .name { font: 600 1.15rem var(--serif); color: var(--blue-strong); }
    .he { color: var(--gold-ink); font-family: var(--serif); }
    .empty { text-align: center; padding: 40px 0; }
  `,
})
export class ParashaList {
  private readonly api = inject(Api);
  protected readonly items = signal<Parasha[]>([]);
  protected readonly loading = signal(true);
  protected readonly groups = computed(() => {
    const map = new Map<string, Parasha[]>();
    for (const p of this.items()) map.set(p.book, [...(map.get(p.book) ?? []), p]);
    return [...map].map(([book, items]) => ({ book, items }));
  });

  constructor() {
    this.api.publicParashot().subscribe({
      next: (p) => { this.items.set(p); this.loading.set(false); },
      error: () => this.loading.set(false),
    });
  }
}
