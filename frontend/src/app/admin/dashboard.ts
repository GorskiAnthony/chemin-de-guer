import { DatePipe } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { Api, Article, Comment, Fete, Parasha, TagRef } from '../core/api';
import { Auth } from '../core/auth';

@Component({
  selector: 'app-dashboard',
  imports: [RouterLink, DatePipe],
  template: `
    <div class="head">
      <h1>Administration</h1>
      <div>
        <a class="btn gold" routerLink="/admin/articles/nouveau">Nouvel article</a>
        <a class="btn secondary" routerLink="/admin/parashot/nouveau">Nouvelle paracha</a>
        <a class="btn secondary" routerLink="/admin/fetes/nouveau">Nouvelle fête</a>
        <a class="btn ghost" routerLink="/admin/statistiques">Statistiques</a>
        <button class="btn ghost" (click)="logout()">Déconnexion</button>
      </div>
    </div>

    <h2>Articles</h2>
    @for (a of articles(); track a.id) {
      <div class="card row reveal">
        <div>
          <strong>{{ a.title }}</strong>
          <span class="badge" [class.published]="a.statut === 'PUBLISHED'" [class.draft]="a.statut !== 'PUBLISHED'">{{ label(a.statut) }}</span>
          <div class="muted">Modifié le {{ a.updatedAt | date: 'd MMM y, HH:mm' : undefined : 'fr' }}</div>
        </div>
        <div>
          <a class="btn small ghost" [routerLink]="['/admin/articles', a.id]" [attr.aria-label]="'Modifier ' + a.title">Modifier</a>
          <button class="btn small danger" (click)="removeArticle(a)" [attr.aria-label]="'Supprimer ' + a.title">Supprimer</button>
        </div>
      </div>
    } @empty { <p class="muted">Aucun article.</p> }

    <h2>Paracha</h2>
    @for (p of parashot(); track p.id) {
      <div class="card row reveal">
        <div>
          <strong>{{ p.name }}</strong>
          <span class="badge" [class.published]="p.statut === 'PUBLISHED'" [class.draft]="p.statut !== 'PUBLISHED'">{{ label(p.statut) }}</span>
          <div class="muted">{{ p.book }}</div>
        </div>
        <div>
          <a class="btn small ghost" [routerLink]="['/admin/parashot', p.id]" [attr.aria-label]="'Modifier ' + p.name">Modifier</a>
          <button class="btn small danger" (click)="removeParasha(p)" [attr.aria-label]="'Supprimer ' + p.name">Supprimer</button>
        </div>
      </div>
    } @empty { <p class="muted">Aucun résumé de paracha.</p> }

    <h2>Fêtes</h2>
    @for (f of fetes(); track f.id) {
      <div class="card row reveal">
        <div>
          <strong>{{ f.name }}</strong>
          <span class="badge" [class.published]="f.statut === 'PUBLISHED'" [class.draft]="f.statut !== 'PUBLISHED'">{{ label(f.statut) }}</span>
          <div class="muted">{{ f.category }}</div>
        </div>
        <div>
          <a class="btn small ghost" [routerLink]="['/admin/fetes', f.id]" [attr.aria-label]="'Modifier ' + f.name">Modifier</a>
          <button class="btn small danger" (click)="removeFete(f)" [attr.aria-label]="'Supprimer ' + f.name">Supprimer</button>
        </div>
      </div>
    } @empty { <p class="muted">Aucun résumé de fête.</p> }

    <div class="com-head">
      <h2>Commentaires
        @if (pendingCount() > 0) { <span class="badge pending">{{ pendingCount() }} en attente</span> }
      </h2>
      <button class="btn small" [class.secondary]="commentsEnabled()" [class.danger]="!commentsEnabled()" (click)="toggleComments()">
        {{ commentsEnabled() ? 'Fermer tous les commentaires' : 'Rouvrir les commentaires' }}
      </button>
    </div>
    @if (!commentsEnabled()) {
      <p class="card notice">Les commentaires sont fermés sur tout le site : personne ne peut en envoyer. Ceux déjà approuvés restent visibles.</p>
    }
    <div class="filters" role="group" aria-label="Filtrer les commentaires">
      <button class="tag" [class.on]="filter() === 'PENDING'" [attr.aria-pressed]="filter() === 'PENDING'" (click)="filter.set('PENDING')">À modérer ({{ pendingCount() }})</button>
      <button class="tag" [class.on]="filter() === 'APPROVED'" [attr.aria-pressed]="filter() === 'APPROVED'" (click)="filter.set('APPROVED')">Approuvés</button>
      <button class="tag" [class.on]="filter() === 'ALL'" [attr.aria-pressed]="filter() === 'ALL'" (click)="filter.set('ALL')">Tous</button>
    </div>
    @for (c of visibleComments(); track c.id) {
      <div class="card row reveal">
        <div>
          <strong>{{ c.authorName }}</strong> sur « {{ c.articleTitle }} »
          <span class="badge" [class.pending]="c.statut === 'PENDING'" [class.published]="c.statut !== 'PENDING'">{{ c.statut === 'PENDING' ? 'En attente' : 'Approuvé' }}</span>
          <div class="muted">{{ c.createdAt | date: 'd MMM y, HH:mm' : undefined : 'fr' }}</div>
          <div class="msg">{{ c.content }}</div>
        </div>
        <div>
          @if (c.statut === 'PENDING') { <button class="btn small" (click)="approve(c)" [attr.aria-label]="'Approuver le commentaire de ' + c.authorName">Approuver</button> }
          <button class="btn small danger" (click)="removeComment(c)" [attr.aria-label]="'Supprimer le commentaire de ' + c.authorName">Supprimer</button>
        </div>
      </div>
    } @empty { <p class="muted">{{ filter() === 'PENDING' ? 'Aucun commentaire à modérer.' : 'Aucun commentaire.' }}</p> }

    <h2>Tags</h2>
    <div>
      @for (t of tags(); track t.id) {
        <span class="tag">{{ t.name }} <button class="x" (click)="removeTag(t)" [attr.aria-label]="'Supprimer ' + t.name">×</button></span>
      }
    </div>
    <form class="newtag" (submit)="addTag($event)">
      <input aria-label="Nouveau tag" placeholder="Nouveau tag" maxlength="60" [value]="newTag()" (input)="newTag.set($any($event.target).value)">
      <button class="btn small" type="submit">Ajouter</button>
    </form>
  `,
  styles: `
    h2 { margin-top: 2.2rem; font-size: 1.5rem; }
    .row .muted { font: 400 .78rem var(--mono); margin-top: 4px; }
    .head { display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: .5rem; }
    .head .btn { margin-inline-start: .5rem; }
    .row { display: flex; justify-content: space-between; gap: 1rem; margin-bottom: .6rem; flex-wrap: wrap; }
    .row .btn { margin-inline-start: .4rem; }
    .msg { white-space: pre-line; margin-top: .3rem; }
    .x { min-width: 24px; min-height: 24px; border: 0; background: none; cursor: pointer; color: inherit; font-size: 1rem; }
    .com-head { display: flex; justify-content: space-between; align-items: center; gap: 12px; flex-wrap: wrap; }
    .com-head h2 { display: flex; align-items: center; gap: 12px; flex-wrap: wrap; }
    .filters { margin-bottom: 14px; }
    button.tag { border: 0; cursor: pointer; font-family: var(--mono); }
    .notice { margin-bottom: 14px; border-left: 3px solid var(--danger); }
    .newtag { display: flex; gap: .5rem; max-width: 360px; margin-top: .6rem; }
  `,
})
export class Dashboard {
  private readonly api = inject(Api);
  private readonly auth = inject(Auth);
  private readonly router = inject(Router);
  protected readonly articles = signal<Article[]>([]);
  protected readonly comments = signal<Comment[]>([]);
  protected readonly tags = signal<TagRef[]>([]);
  protected readonly parashot = signal<Parasha[]>([]);
  protected readonly fetes = signal<Fete[]>([]);
  protected readonly newTag = signal('');
  protected readonly commentsEnabled = signal(true);
  protected readonly filter = signal<'PENDING' | 'APPROVED' | 'ALL'>('PENDING');
  protected readonly pendingCount = computed(() => this.comments().filter((c) => c.statut === 'PENDING').length);
  protected readonly visibleComments = computed(() => {
    const f = this.filter();
    const list = this.comments().filter((c) => f === 'ALL' || c.statut === f);
    // À modérer d'abord, puis du plus récent au plus ancien
    return [...list].sort((a, b) =>
      (a.statut === 'PENDING' ? 0 : 1) - (b.statut === 'PENDING' ? 0 : 1) || b.createdAt.localeCompare(a.createdAt));
  });

  constructor() { this.reload(); }

  protected label(s: string) { return s === 'PUBLISHED' ? 'Publié' : s === 'DRAFT' ? 'Brouillon' : 'Archivé'; }

  private reload() {
    this.api.adminArticles().subscribe((a) => this.articles.set(a));
    this.api.adminComments().subscribe((c) => this.comments.set(c));
    this.api.publicTags().subscribe((t) => this.tags.set(t));
    this.api.adminParashot().subscribe((p) => this.parashot.set(p));
    this.api.adminFetes().subscribe((f) => this.fetes.set(f));
    this.api.settings().subscribe((st) => this.commentsEnabled.set(st.commentsEnabled));
  }

  protected removeArticle(a: Article) {
    if (!confirm(`Supprimer « ${a.title} » ?`)) return;
    this.api.deleteArticle(a.id).subscribe(() => this.reload());
  }
  protected removeParasha(p: Parasha) {
    if (!confirm(`Supprimer « ${p.name} » ?`)) return;
    this.api.deleteParasha(p.id).subscribe(() => this.reload());
  }
  protected toggleComments() {
    const next = !this.commentsEnabled();
    if (!next && !confirm('Fermer les commentaires sur tout le site ?')) return;
    this.api.updateSettings({ commentsEnabled: next }).subscribe((st) => this.commentsEnabled.set(st.commentsEnabled));
  }
  protected removeFete(f: Fete) {
    if (!confirm(`Supprimer « ${f.name} » ?`)) return;
    this.api.deleteFete(f.id).subscribe(() => this.reload());
  }
  protected approve(c: Comment) { this.api.approveComment(c.id).subscribe(() => this.reload()); }
  protected removeComment(c: Comment) {
    if (!confirm('Supprimer ce commentaire ?')) return;
    this.api.deleteComment(c.id).subscribe(() => this.reload());
  }
  protected addTag(e: Event) {
    e.preventDefault();
    const name = this.newTag().trim();
    if (!name) return;
    this.api.createTag(name).subscribe(() => { this.newTag.set(''); this.reload(); });
  }
  protected removeTag(t: TagRef) {
    if (!confirm(`Supprimer le tag « ${t.name} » ?`)) return;
    this.api.deleteTag(t.id).subscribe(() => this.reload());
  }
  protected logout() { this.auth.logout().subscribe(() => this.router.navigateByUrl('/')); }
}
