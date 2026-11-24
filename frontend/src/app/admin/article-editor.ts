import { Component, computed, effect, inject, input, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { Api, Statut, TagRef } from '../core/api';
import { renderMarkdown } from '../core/markdown';

@Component({
  selector: 'app-article-editor',
  imports: [RouterLink],
  template: `
    <a routerLink="/admin">← Retour</a>
    <h1>{{ id() ? "Modifier l'article" : 'Nouvel article' }}</h1>
    <form (submit)="save($event)">
      <label for="t">Titre</label>
      <input id="t" required maxlength="200" [value]="title()" (input)="title.set($any($event.target).value)">

      <label for="sl">Adresse (slug)</label>
      <input id="sl" maxlength="100" pattern="[a-z0-9]+(-[a-z0-9]+)*" placeholder="générée depuis le titre si vide"
             [value]="slug()" (input)="slug.set($any($event.target).value)">
      <p class="hint">Lettres minuscules, chiffres et tirets. Ex : <code>mon-premier-shabbat</code>. Le lien public sera <code>/articles/{{ slug() || '…' }}</code>.</p>

      <label for="s">Statut</label>
      <select id="s" [value]="statut()" (change)="statut.set($any($event.target).value)">
        <option value="DRAFT">Brouillon</option>
        <option value="PUBLISHED">Publié</option>
        <option value="ARCHIVED">Archivé</option>
      </select>

      <label class="check commentaires"><input type="checkbox" [checked]="commentsOpen()" (change)="commentsOpen.set($any($event.target).checked)">
        Autoriser les commentaires sur cet article</label>

      <label>Tags</label>
      @for (t of tags(); track t.id) {
        <label class="check"><input type="checkbox" [checked]="tagIds().includes(t.id)" (change)="toggle(t.id)"> {{ t.name }}</label>
      }

      <div class="split">
        <div>
          <label for="c">Contenu (Markdown : ## titre, **gras**, *italique*, - liste, > citation)</label>
          <textarea id="c" rows="18" required [value]="content()" (input)="content.set($any($event.target).value)"></textarea>
        </div>
        <div>
          <label>Aperçu</label>
          <div class="card preview" [innerHTML]="preview()"></div>
        </div>
      </div>

      @if (error()) { <p class="error">{{ error() }}</p> }
      <p><button class="btn" type="submit" [disabled]="busy()">Enregistrer</button></p>
    </form>
  `,
  styles: `
    .check { display: inline-flex; align-items: center; gap: .3rem; margin-inline-end: 1rem; font-weight: 400; }
    .check input { width: auto; }
    .commentaires { margin-top: 18px; text-transform: none; letter-spacing: 0; font: 500 .95rem 'Work Sans', sans-serif; color: var(--ink); }
    .split { display: grid; grid-template-columns: 1fr 1fr; gap: 1rem; }
    @media (max-width: 800px) { .split { grid-template-columns: 1fr; } }
    .preview { min-height: 12rem; }
    .hint { margin: 6px 0 0; font-size: .85rem; color: var(--ink-muted); }
  `,
})
export class ArticleEditor {
  readonly id = input<string>();
  private readonly api = inject(Api);
  private readonly router = inject(Router);

  protected readonly title = signal('');
  protected readonly slug = signal('');
  protected readonly commentsOpen = signal(true);
  protected readonly content = signal('');
  protected readonly statut = signal<Statut>('DRAFT');
  protected readonly tagIds = signal<number[]>([]);
  protected readonly tags = signal<TagRef[]>([]);
  protected readonly busy = signal(false);
  protected readonly error = signal('');
  protected readonly preview = computed(() => renderMarkdown(this.content()));

  constructor() {
    this.api.publicTags().subscribe((t) => this.tags.set(t));
    effect(() => {
      const id = this.id();
      if (!id) return;
      this.api.adminArticle(Number(id)).subscribe((a) => {
        this.title.set(a.title);
        this.slug.set(a.slug ?? '');
        this.commentsOpen.set(a.commentsOpen);
        this.content.set(a.content ?? '');
        this.statut.set(a.statut);
        this.tagIds.set(a.tags.map((t) => t.id));
      });
    });
  }

  protected toggle(id: number) {
    this.tagIds.update((ids) => (ids.includes(id) ? ids.filter((i) => i !== id) : [...ids, id]));
  }

  protected save(e: Event) {
    e.preventDefault();
    this.busy.set(true);
    this.error.set('');
    const input = { title: this.title(), slug: this.slug().trim(), content: this.content(), statut: this.statut(), tagIds: this.tagIds(), commentsOpen: this.commentsOpen() };
    const id = this.id();
    const request = id ? this.api.updateArticle(Number(id), input) : this.api.createArticle(input);
    request.subscribe({
      next: () => this.router.navigateByUrl('/admin'),
      error: () => { this.busy.set(false); this.error.set("L'enregistrement a échoué."); },
    });
  }
}
