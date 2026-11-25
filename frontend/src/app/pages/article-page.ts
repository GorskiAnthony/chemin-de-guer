import { DatePipe } from '@angular/common';
import { Component, computed, effect, inject, input, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { Api, Article, Comment } from '../core/api';
import { renderMarkdown } from '../core/markdown';
import { Seo } from '../core/seo';
import { SITE } from '../core/site';
import { Share } from './share';

@Component({
  selector: 'app-article-page',
  imports: [RouterLink, DatePipe, Share],
  template: `
    <div class="narrow">
    <a class="back" routerLink="/articles">← Tous les articles</a>
    @if (notFound()) { <p class="card" role="alert">Article introuvable.</p> }
    @if (article(); as a) {
      <article class="reveal sheet">
        <header>
          <h1>{{ a.title }}</h1>
          <p class="byline">
            Par <a [href]="site.authorUrl" target="_blank" rel="noopener noreferrer">{{ site.author }}</a>
            <span aria-hidden="true"> · </span>
            <time [attr.datetime]="a.publishedAt">{{ a.publishedAt | date: 'd MMMM y' : undefined : 'fr' }}</time>
            <span aria-hidden="true"> · </span>{{ readingTime() }} min de lecture
          </p>
          @if (updated()) {
            <p class="byline">Mis à jour le <time [attr.datetime]="a.updatedAt">{{ a.updatedAt | date: 'd MMMM y' : undefined : 'fr' }}</time></p>
          }
          <div>@for (t of a.tags; track t.id) { <span class="tag">#{{ t.name }}</span> }</div>
        </header>
        <div class="body" [innerHTML]="html()"></div>
        <app-share [path]="'/articles/' + (a.slug ?? a.id)" [title]="a.title" />
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
            <p class="msg">{{ c.content }}</p></div>
        } @empty { <p class="muted">Aucun commentaire pour le moment.</p> }

        @if (!a.commentsOpen) {
          <p class="card closed">Les commentaires sont fermés sur cet article.</p>
        } @else {
        <form class="card form" (submit)="submit($event)">
          <h3>Laisser un commentaire</h3>
          <label for="name">Nom</label>
          <input id="name" maxlength="80" required autocomplete="nickname" [value]="name()" (input)="name.set($any($event.target).value)">
          <label for="msg">Message</label>
          <textarea id="msg" rows="4" maxlength="2000" required [value]="message()"
                    (input)="message.set($any($event.target).value)"></textarea>
          <input class="hp" tabindex="-1" autocomplete="off" aria-hidden="true" name="website"
                 [value]="website()" (input)="website.set($any($event.target).value)">
          <p><button class="btn" type="submit" [disabled]="sending()">Envoyer</button></p>
          @if (sent()) { <p class="muted" role="status">Merci ! Votre commentaire sera visible après modération.</p> }
          @if (failed()) { <p class="error" role="alert">{{ failed() }}</p> }
        </form>
        }
      </section>
    }
    </div>
  `,
  styles: `
    .back { display: inline-block; margin: 28px 0 8px; font-size: .92rem; text-decoration: none; color: var(--ink-muted); }
    .byline { margin: 0 0 4px; font-size: .92rem; color: var(--ink-muted); }
    section { margin-top: 3rem; padding-top: 1.5rem; border-top: 1px solid var(--border); }
    section h2 { font-size: 1.5rem; }
    .rel { margin: .4rem 0; font: 600 1.05rem var(--serif); }
    .form { margin-top: 1.5rem; }
    .closed { margin-top: 1.5rem; color: var(--ink-muted); }
    .form h3 { margin: 0; }
    .msg { white-space: pre-line; overflow-wrap: anywhere; }
    .hp { position: absolute; left: -9999px; width: 1px; height: 1px; opacity: 0; }
  `,
})
export class ArticlePage {
  readonly slug = input.required<string>();
  private readonly api = inject(Api);
  private readonly seo = inject(Seo);
  protected readonly site = SITE;
  private readonly router = inject(Router);

  protected readonly article = signal<Article | null>(null);
  protected readonly related = signal<Article[]>([]);
  protected readonly comments = signal<Comment[]>([]);
  protected readonly notFound = signal(false);
  protected readonly html = computed(() => renderMarkdown(this.article()?.content ?? ''));
  protected readonly readingTime = computed(() => Math.max(1, Math.round((this.article()?.content ?? '').split(/\s+/).length / 200)));
  protected readonly updated = computed(() => {
    const a = this.article();
    return !!a && !!a.publishedAt && new Date(a.updatedAt).getTime() - new Date(a.publishedAt).getTime() > 86_400_000;
  });

  protected readonly name = signal('');
  protected readonly message = signal('');
  protected readonly website = signal('');
  protected readonly sending = signal(false);
  protected readonly sent = signal(false);
  protected readonly failed = signal('');

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
          this.seo.set({
            title: `${a.title} · ${SITE.name}`,
            description: a.excerpt,
            path: `/articles/${a.slug ?? a.id}`,
            image: a.slug ? `/og/articles/${a.slug}.png` : undefined,
            type: 'article',
          });
          this.api.related(a.id).subscribe({ next: (r) => this.related.set(r), error: () => this.related.set([]) });
          this.loadComments(a.id);
        },
        error: () => { this.notFound.set(true); this.seo.set({ title: `Article introuvable · ${SITE.name}`, description: 'Cet article n’existe pas.', path: '/articles', noindex: true }); },
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
    this.failed.set('');
    this.api.postComment(this.article()!.id, {
      authorName: this.name(), content: this.message(), website: this.website(),
    }).subscribe({
      next: () => { this.sending.set(false); this.sent.set(true); this.name.set(''); this.message.set(''); },
      error: (err) => {
        this.sending.set(false);
        this.failed.set(
          err.status === 429 ? 'Vous avez envoyé plusieurs commentaires à la suite. Réessayez dans quelques minutes.'
          : err.status === 403 ? 'Les commentaires sont fermés.'
          : "L'envoi a échoué, réessayez plus tard.");
      },
    });
  }
}
