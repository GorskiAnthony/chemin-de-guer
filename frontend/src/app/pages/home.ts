import { DatePipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Api, Article, TagRef } from '../core/api';

@Component({
  selector: 'app-home',
  imports: [RouterLink, DatePipe],
  template: `
    <h1>Journal de conversion</h1>
    <p class="muted">Un chemin vers le judaïsme, étape après étape.</p>

    <div class="filters">
      <input type="search" placeholder="Rechercher un article" aria-label="Rechercher"
             [value]="query()" (input)="onSearch($any($event.target).value)">
      <div class="tags">
        <button class="tag" [class.on]="!tag()" (click)="selectTag('')">Tous</button>
        @for (t of tags(); track t.id) {
          <button class="tag" [class.on]="tag() === t.slug" (click)="selectTag(t.slug)">{{ t.name }}</button>
        }
      </div>
    </div>

    @if (loading()) { <p class="muted">Chargement…</p> }
    @if (!loading() && articles().length === 0) { <p class="muted">Aucun article pour le moment.</p> }
    @for (a of articles(); track a.id) {
      <article class="card item reveal">
        <h2><a [routerLink]="['/articles', a.id]">{{ a.title }}</a></h2>
        <p class="muted meta">{{ a.publishedAt | date: 'd MMMM y' : undefined : 'fr' }}</p>
        <p>{{ a.excerpt }}</p>
        @for (t of a.tags; track t.id) { <span class="tag">{{ t.name }}</span> }
      </article>
    }
  `,
  styles: `
    .filters { margin: 1.5rem 0; }
    .tags { margin-top: .8rem; }
    button.tag { border: 0; cursor: pointer; font-family: inherit; }
    button.tag.on { background: var(--blue); color: var(--surface); }
    .item { margin-bottom: 1rem; }
    .item h2 { margin: 0 0 .2rem; font-size: 1.5rem; }
    .item h2 a { text-decoration: none; }
    .meta { margin: 0 0 .6rem; font-size: .85rem; }
  `,
})
export class Home {
  private readonly api = inject(Api);
  protected readonly articles = signal<Article[]>([]);
  protected readonly tags = signal<TagRef[]>([]);
  protected readonly tag = signal('');
  protected readonly query = signal('');
  protected readonly loading = signal(true);
  private timer?: ReturnType<typeof setTimeout>;

  constructor() {
    this.api.publicTags().subscribe({ next: (t) => this.tags.set(t), error: () => {} });
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
