import { DatePipe } from '@angular/common';
import { Component, computed, effect, inject, input, signal } from '@angular/core';
import { Meta, Title } from '@angular/platform-browser';
import { RouterLink } from '@angular/router';
import { Api, Article, Comment } from '../core/api';
import { renderMarkdown } from '../core/markdown';

@Component({
  selector: 'app-article-page',
  imports: [RouterLink, DatePipe],
  template: `
    <a routerLink="/">← Tous les articles</a>
    @if (notFound()) { <p class="card">Article introuvable.</p> }
    @if (article(); as a) {
      <article>
        <h1>{{ a.title }}</h1>
        <p class="muted">{{ a.publishedAt | date: 'd MMMM y' : undefined : 'fr' }}</p>
        @for (t of a.tags; track t.id) { <span class="tag">{{ t.name }}</span> }
        <div class="body" [innerHTML]="html()"></div>
      </article>

      @if (related().length) {
        <section>
          <h2>À lire aussi</h2>
          @for (r of related(); track r.id) {
            <p><a [routerLink]="['/articles', r.id]">{{ r.title }}</a></p>
          }
        </section>
      }

      <section>
        <h2>Commentaires</h2>
        @for (c of comments(); track c.id) {
          <div class="card comment"><strong>{{ c.authorName }}</strong>
            <span class="muted"> · {{ c.createdAt | date: 'd MMMM y' : undefined : 'fr' }}</span>
            <p>{{ c.content }}</p></div>
        } @empty { <p class="muted">Aucun commentaire pour le moment.</p> }

        <form class="card" (submit)="submit($event)">
          <h3>Laisser un commentaire</h3>
          <label for="name">Nom</label>
          <input id="name" maxlength="80" required [value]="name()" (input)="name.set($any($event.target).value)">
          <label for="msg">Message</label>
          <textarea id="msg" rows="4" maxlength="2000" required [value]="message()"
                    (input)="message.set($any($event.target).value)"></textarea>
          <input class="hp" tabindex="-1" autocomplete="off" aria-hidden="true" name="website"
                 [value]="website()" (input)="website.set($any($event.target).value)">
          <p><button class="btn" type="submit" [disabled]="sending()">Envoyer</button></p>
          @if (sent()) { <p class="muted">Merci ! Votre commentaire sera visible après modération.</p> }
          @if (failed()) { <p class="error">L'envoi a échoué, réessayez plus tard.</p> }
        </form>
      </section>
    }
  `,
  styles: `
    .body { margin: 1.5rem 0; font-size: 1.08rem; }
    .body :is(h2, h3, h4) { margin-top: 2rem; }
    .body blockquote { border-inline-start: 4px solid var(--gold); margin-inline: 0; padding-inline-start: 1rem; color: var(--ink-muted); }
    section { margin-top: 2.5rem; }
    .comment { margin-bottom: .8rem; }
    .comment p { margin: .4rem 0 0; white-space: pre-line; }
    .hp { position: absolute; left: -9999px; width: 1px; height: 1px; opacity: 0; }
  `,
})
export class ArticlePage {
  readonly id = input.required<string>();
  private readonly api = inject(Api);
  private readonly title = inject(Title);
  private readonly meta = inject(Meta);

  protected readonly article = signal<Article | null>(null);
  protected readonly related = signal<Article[]>([]);
  protected readonly comments = signal<Comment[]>([]);
  protected readonly notFound = signal(false);
  protected readonly html = computed(() => renderMarkdown(this.article()?.content ?? ''));

  protected readonly name = signal('');
  protected readonly message = signal('');
  protected readonly website = signal('');
  protected readonly sending = signal(false);
  protected readonly sent = signal(false);
  protected readonly failed = signal(false);

  constructor() {
    effect(() => {
      const id = Number(this.id());
      this.notFound.set(false);
      this.api.publicArticle(id).subscribe({
        next: (a) => {
          this.article.set(a);
          this.title.setTitle(`${a.title} · Chemin de Guer`);
          this.meta.updateTag({ name: 'description', content: a.excerpt });
          this.meta.updateTag({ property: 'og:title', content: a.title });
          this.meta.updateTag({ property: 'og:description', content: a.excerpt });
        },
        error: () => this.notFound.set(true),
      });
      this.api.related(id).subscribe({ next: (r) => this.related.set(r), error: () => this.related.set([]) });
      this.loadComments(id);
    });
  }

  private loadComments(id: number) {
    this.api.comments(id).subscribe({ next: (c) => this.comments.set(c), error: () => this.comments.set([]) });
  }

  protected submit(event: Event) {
    event.preventDefault();
    if (!this.name().trim() || !this.message().trim()) return;
    this.sending.set(true);
    this.sent.set(false);
    this.failed.set(false);
    this.api.postComment(Number(this.id()), {
      authorName: this.name(), content: this.message(), website: this.website(),
    }).subscribe({
      next: () => { this.sending.set(false); this.sent.set(true); this.name.set(''); this.message.set(''); },
      error: () => { this.sending.set(false); this.failed.set(true); },
    });
  }
}
