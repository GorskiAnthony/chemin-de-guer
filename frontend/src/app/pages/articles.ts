import { DatePipe } from '@angular/common';
import { Component, OnInit, inject, input, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Api, Article, TagRef } from '../core/api';
import { Seo } from '../core/seo';

@Component({
  selector: 'app-articles',
  imports: [RouterLink, DatePipe],
  template: `
    <section class="hero">
      <p class="caption">Journal de conversion</p>
      <h1>Articles</h1>
      <p class="sub">Les doutes, les rencontres, les étapes qui comptent.</p>
    </section>

    <section class="filters">
      <input type="search" placeholder="Rechercher un article" aria-label="Rechercher un article"
             [value]="query()" (input)="onSearch($any($event.target).value)">
      <div class="tags" role="group" aria-label="Filtrer par tag">
        <button class="tag" [class.on]="!tag()" [attr.aria-pressed]="!tag()" (click)="selectTag('')">Tous</button>
        @for (t of tags(); track t.id) {
          <button class="tag" [class.on]="tag() === t.slug" [attr.aria-pressed]="tag() === t.slug" (click)="selectTag(t.slug)">#{{ t.name }}</button>
        }
      </div>
    </section>

    <p class="sr-only" role="status" aria-live="polite">{{ loading() ? '' : articles().length + (articles().length > 1 ? ' articles trouvés' : ' article trouvé') }}</p>
    @if (!loading() && articles().length === 0) { <p class="muted empty">Aucun article pour le moment.</p> }
    <div class="grid">
      @for (a of articles(); track a.id) {
        <article class="card article-card reveal">
          <div class="stripe-card"></div>
          <div class="body">
            <span class="mono">{{ a.publishedAt | date: 'd MMMM y' : undefined : 'fr' }}</span>
            <h3><a [routerLink]="['/articles', a.slug ?? a.id]">{{ a.title }}</a></h3>
            <p>{{ a.excerpt }}</p>
            <div>@for (t of a.tags; track t.id) { <span class="tag">#{{ t.name }}</span> }</div>
          </div>
        </article>
      }
    </div>
  `,
  styles: `
    .hero { text-align: center; padding: 44px 0 28px; }
    h1 { font-size: clamp(2.2rem, 6vw, 3.4rem); margin: 8px 0 0; }
    .sub { max-width: 52ch; margin: 16px auto 0; color: var(--ink-muted); font-size: 1.05rem; }
    .filters { margin-bottom: 28px; padding-bottom: 20px; border-bottom: 1px solid var(--border); }
    .filters input { max-width: 360px; margin-bottom: 14px; }
    .grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(260px, 1fr)); gap: 20px; }
    .empty { text-align: center; padding: 40px 0; }
  `,
})
export class Articles implements OnInit {
  /** Recherche transmise par l'adresse : /articles?q=... (utilisée par les moteurs de recherche) */
  readonly q = input<string>();
  private readonly api = inject(Api);
  protected readonly articles = signal<Article[]>([]);
  protected readonly tags = signal<TagRef[]>([]);
  protected readonly tag = signal('');
  protected readonly query = signal('');
  protected readonly loading = signal(true);
  private timer?: ReturnType<typeof setTimeout>;

  constructor() {
    inject(Seo).set({
      title: 'Articles · Chemin de Guer',
      description: "Tous les articles du journal : témoignages et étapes d'un parcours de conversion au judaïsme.",
      path: '/articles',
    });
    this.api.publicTags().subscribe({ next: (t) => this.tags.set(t), error: () => {} });
  }

  ngOnInit() {
    this.query.set(this.q() ?? '');
    this.load();
  }

  protected selectTag(slug: string) { this.tag.set(slug); this.load(); }

  protected onSearch(value: string) {
    this.query.set(value);
    clearTimeout(this.timer);
    this.timer = setTimeout(() => this.load(), 300);
  }

  private load() {
    this.loading.set(true);
    this.api.publicArticles(this.tag(), this.query().trim()).subscribe({
      next: (a) => { this.articles.set(a); this.loading.set(false); },
      error: () => { this.articles.set([]); this.loading.set(false); },
    });
  }
}
