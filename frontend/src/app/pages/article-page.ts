import { DatePipe } from '@angular/common';
import { Component, computed, effect, inject, input, signal } from '@angular/core';
import { Meta, Title } from '@angular/platform-browser';
import { Router, RouterLink } from '@angular/router';
import { Api, Article, Comment } from '../core/api';
import { renderMarkdown } from '../core/markdown';

@Component({
  selector: 'app-article-page',
  imports: [RouterLink, DatePipe],
  template: `
    <div class="narrow">
    <a class="back" routerLink="/">← Tous les articles</a>
    @if (notFound()) { <p class="card">Article introuvable.</p> }
    @if (article(); as a) {
      <article class="reveal sheet">
        <header>
          <p class="mono">{{ a.publishedAt | date: 'd MMMM y' : undefined : 'fr' }}</p>
          <h1>{{ a.title }}</h1>
          <div>@for (t of a.tags; track t.id) { <span class="tag">#{{ t.name }}</span> }</div>
        </header>
        <div class="body" [innerHTML]="html()"></div>
      </article>

      @if (related().length) {
        <section>
          <h2>À lire aussi</h2>
          @for (r of related(); track r.id) {
            <p class="rel"><a [routerLink]="['/articles', r.slug ?? r.id]">{{ r.title }}</a></p>
          }
        </section>
      }

      <section>
        <h2>Commentaires</h2>
        @for (c of comments(); track c.id) {
          <div class="comment"><strong>{{ c.authorName }}</strong>
            <span class="mono"> · {{ c.createdAt | date: 'd MMMM y' : undefined : 'fr' }}</span>
            <p>{{ c.content }}</p></div>
        } @empty { <p class="muted">Aucun commentaire pour le moment.</p> }

        <form class="card form" (submit)="submit($event)">
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
    </div>
  `,
  styles: `
    .back { display: inline-block; margin: 28px 0 8px; font-size: .92rem; text-decoration: none; color: var(--ink-muted); }
    .sheet { background: var(--surface); border: 1px solid var(--border); border-radius: 14px; box-shadow: var(--shadow);
      padding: clamp(1.3rem, 4.5vw, 3rem); }
    header { margin-bottom: 28px; }
    h1 { font-size: clamp(2rem, 5vw, 3.1rem); margin: 6px 0 14px; }
    .body { font-size: 1.12rem; line-height: 1.75; }
    :host ::ng-deep .body :is(h2, h3, h4) { margin-top: 2.2rem; }
    :host ::ng-deep .body blockquote { border-inline-start: 3px solid var(--gold); margin-inline: 0; padding-inline-start: 1.1rem; color: var(--ink-muted); font-family: var(--serif); font-size: 1.25rem; }
    section { margin-top: 3rem; padding-top: 1.5rem; border-top: 1px solid var(--border); }
    section h2 { font-size: 1.5rem; }
    .rel { margin: .4rem 0; font: 600 1.05rem var(--serif); }
    .form { margin-top: 1.5rem; }
    .form h3 { margin: 0; }
    .hp { position: absolute; left: -9999px; width: 1px; height: 1px; opacity: 0; }
  `,
})
export class ArticlePage {
  readonly slug = input.required<string>();
  private readonly api = inject(Api);
  private readonly title = inject(Title);
  private readonly meta = inject(Meta);
  private readonly router = inject(Router);

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
      const key = this.slug();
      this.notFound.set(false);
      this.api.publicArticle(key).subscribe({
        next: (a) => {
          // Ancien lien par identifiant (/articles/1) : on redirige vers l'adresse lisible
          if (a.slug && key !== a.slug) {
            this.router.navigate(['/articles', a.slug], { replaceUrl: true });
            return;
          }
          this.article.set(a);
          this.title.setTitle(`${a.title} · Chemin de Guer`);
          this.meta.updateTag({ name: 'description', content: a.excerpt });
          this.meta.updateTag({ property: 'og:title', content: a.title });
          this.meta.updateTag({ property: 'og:description', content: a.excerpt });
          this.api.related(a.id).subscribe({ next: (r) => this.related.set(r), error: () => this.related.set([]) });
          this.loadComments(a.id);
        },
        error: () => this.notFound.set(true),
      });
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
    this.api.postComment(this.article()!.id, {
      authorName: this.name(), content: this.message(), website: this.website(),
    }).subscribe({
      next: () => { this.sending.set(false); this.sent.set(true); this.name.set(''); this.message.set(''); },
      error: () => { this.sending.set(false); this.failed.set(true); },
    });
  }
}
