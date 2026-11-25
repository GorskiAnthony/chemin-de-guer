import { Component, effect, inject, input, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { Api, FeteCatalogEntry, FeteSection, Statut } from '../core/api';

const DEFAULT_SECTIONS = [
  'Histoire et origine',
  'Date et durée',
  'Coutumes et rituels',
  'Prières et lectures',
  "Mon vécu et ce que j'ai appris",
];

@Component({
  selector: 'app-fete-editor',
  imports: [RouterLink],
  template: `
    <a routerLink="/admin">← Retour</a>
    <h1>{{ id() ? 'Modifier la fête' : 'Nouvelle fête' }}</h1>
    <form (submit)="save($event)">
      <div class="two">
        <div>
          <label for="n">Nom</label>
          <input id="n" list="catalog" required maxlength="80" [value]="name()" (input)="onName($any($event.target).value)">
          <datalist id="catalog">@for (c of catalog(); track c.name) { <option [value]="c.name">{{ c.category }}</option> }</datalist>
        </div>
        <div>
          <label for="h">Nom en hébreu (rempli automatiquement)</label>
          <input id="h" dir="rtl" lang="he" maxlength="80" [value]="hebrewName()" (input)="hebrewName.set($any($event.target).value)">
        </div>
      </div>
      <p class="hint">Choisissez un nom de la liste pour que la fête soit classée automatiquement, et reliée au calendrier de l'accueil avec sa prochaine date. Un autre nom est accepté (catégorie « Autres »).</p>

      <label for="s">Statut</label>
      <select id="s" [value]="statut()" (change)="statut.set($any($event.target).value)">
        <option value="DRAFT">Brouillon</option>
        <option value="PUBLISHED">Publié</option>
      </select>

      <label for="i">Résumé général (Markdown)</label>
      <textarea id="i" rows="6" [value]="intro()" (input)="intro.set($any($event.target).value)"></textarea>

      <div class="head">
        <label>Parties</label>
        <button type="button" class="btn small secondary" (click)="prefill()">Ajouter les parties types</button>
      </div>
      @for (sec of sections(); track $index) {
        <div class="card part">
          <div class="line">
            <input aria-label="Titre de la partie" maxlength="80" placeholder="Titre" [value]="sec.label ?? ''" (input)="setLabel($index, $any($event.target).value)">
            <button type="button" class="btn small danger" (click)="remove($index)">Retirer</button>
          </div>
          <textarea rows="5" aria-label="Contenu de la partie" placeholder="Ce que je veux dire sur ce sujet" [value]="sec.content ?? ''" (input)="setContent($index, $any($event.target).value)"></textarea>
        </div>
      }
      <p><button type="button" class="btn small ghost" (click)="add()">Ajouter une partie</button></p>

      @if (error()) { <p class="error" role="alert">{{ error() }}</p> }
      <p><button class="btn" type="submit" [disabled]="busy()">Enregistrer</button></p>
    </form>
  `,
  styles: `
    .two { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; }
    @media (max-width: 700px) { .two { grid-template-columns: 1fr; } }
    .hint { margin: 6px 0 0; font-size: .85rem; color: var(--ink-muted); }
    .head { display: flex; justify-content: space-between; align-items: end; gap: 10px; flex-wrap: wrap; }
    .part { margin-bottom: 12px; padding: 14px; }
    .line { display: flex; gap: 10px; margin-bottom: 10px; }
  `,
})
export class FeteEditor {
  readonly id = input<string>();
  private readonly api = inject(Api);
  private readonly router = inject(Router);

  protected readonly name = signal('');
  protected readonly hebrewName = signal('');
  protected readonly intro = signal('');
  protected readonly statut = signal<Statut>('DRAFT');
  protected readonly sections = signal<FeteSection[]>([]);
  protected readonly catalog = signal<FeteCatalogEntry[]>([]);
  protected readonly busy = signal(false);
  protected readonly error = signal('');
  private autoHebrew = '';

  constructor() {
    this.api.feteCatalog().subscribe((c) => this.catalog.set(c));
    effect(() => {
      const id = this.id();
      if (!id) return;
      this.api.adminFete(Number(id)).subscribe((f) => {
        this.name.set(f.name);
        this.hebrewName.set(f.hebrewName ?? '');
        this.intro.set(f.intro ?? '');
        this.statut.set(f.statut);
        this.sections.set(f.sections ?? []);
      });
    });
  }

  /** Le nom hébreu se remplit tout seul quand le nom correspond au catalogue, et suit si on change de fête. Une saisie à la main n'est jamais écrasée. */
  protected onName(value: string) {
    this.name.set(value);
    const known = this.catalog().find((c) => this.norm(c.name) === this.norm(value));
    if (known && (this.hebrewName() === '' || this.hebrewName() === this.autoHebrew)) {
      this.hebrewName.set(known.hebrew);
      this.autoHebrew = known.hebrew;
    }
  }

  private norm(s: string) { return s.trim().toLowerCase().replace(/[\u2018\u2019']/g, ''); }

  protected prefill() {
    this.sections.update((current) => [
      ...current,
      ...DEFAULT_SECTIONS.filter((l) => !current.some((c) => c.label === l)).map((label) => ({ label, content: '' })),
    ]);
  }
  protected add() { this.sections.update((a) => [...a, { label: '', content: '' }]); }
  protected remove(i: number) { this.sections.update((a) => a.filter((_, idx) => idx !== i)); }
  protected setLabel(i: number, v: string) { this.sections.update((a) => a.map((x, idx) => (idx === i ? { ...x, label: v } : x))); }
  protected setContent(i: number, v: string) { this.sections.update((a) => a.map((x, idx) => (idx === i ? { ...x, content: v } : x))); }

  protected save(e: Event) {
    e.preventDefault();
    this.busy.set(true);
    this.error.set('');
    const input = { name: this.name(), hebrewName: this.hebrewName(), intro: this.intro(), sections: this.sections(), statut: this.statut() };
    const id = this.id();
    const req = id ? this.api.updateFete(Number(id), input) : this.api.createFete(input);
    req.subscribe({
      next: () => this.router.navigateByUrl('/admin'),
      error: (err) => {
        this.busy.set(false);
        this.error.set(err.status === 409 ? 'Cette fête existe déjà.' : "L'enregistrement a échoué.");
      },
    });
  }
}
