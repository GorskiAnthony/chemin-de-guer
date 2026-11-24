import { Component, effect, inject, input, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { Aliyah, Api, CatalogEntry, Statut } from '../core/api';

const DEFAULT_LABELS = ['1re montée', '2e montée', '3e montée', '4e montée', '5e montée', '6e montée', '7e montée', 'Maftir'];

@Component({
  selector: 'app-parasha-editor',
  imports: [RouterLink],
  template: `
    <a routerLink="/admin">← Retour</a>
    <h1>{{ id() ? 'Modifier la paracha' : 'Nouvelle paracha' }}</h1>
    <form (submit)="save($event)">
      <div class="two">
        <div>
          <label for="n">Nom (orthographe Hebcal)</label>
          <input id="n" list="catalog" required maxlength="80" [value]="name()" (input)="onName($any($event.target).value)">
          <datalist id="catalog">@for (c of catalog(); track c.name) { <option [value]="c.name">{{ c.book }}</option> }</datalist>
        </div>
        <div>
          <label for="h">Nom en hébreu (rempli automatiquement)</label>
          <input id="h" dir="rtl" lang="he" maxlength="80" [value]="hebrewName()" (input)="hebrewName.set($any($event.target).value)">
        </div>
      </div>

      <label for="s">Statut</label>
      <select id="s" [value]="statut()" (change)="statut.set($any($event.target).value)">
        <option value="DRAFT">Brouillon</option>
        <option value="PUBLISHED">Publié</option>
      </select>

      <label for="i">Résumé général (Markdown)</label>
      <textarea id="i" rows="6" [value]="intro()" (input)="intro.set($any($event.target).value)"></textarea>

      <div class="aliyot-head">
        <label>Montées</label>
        <button type="button" class="btn small secondary" (click)="prefill()">Préparer les 7 montées + Maftir</button>
      </div>
      @for (a of aliyot(); track $index) {
        <div class="card aliyah">
          <div class="line">
            <input aria-label="Titre de la montée" maxlength="80" placeholder="Titre" [value]="a.label ?? ''" (input)="setLabel($index, $any($event.target).value)">
            <button type="button" class="btn small danger" (click)="remove($index)">Retirer</button>
          </div>
          <textarea rows="5" aria-label="Contenu de la montée" placeholder="Ce que j'ai retenu de cette montée" [value]="a.content ?? ''" (input)="setContent($index, $any($event.target).value)"></textarea>
        </div>
      }
      <p><button type="button" class="btn small ghost" (click)="add()">Ajouter une montée</button></p>

      @if (error()) { <p class="error">{{ error() }}</p> }
      <p><button class="btn" type="submit" [disabled]="busy()">Enregistrer</button></p>
    </form>
  `,
  styles: `
    .two { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; }
    @media (max-width: 700px) { .two { grid-template-columns: 1fr; } }
    .aliyot-head { display: flex; justify-content: space-between; align-items: end; gap: 10px; flex-wrap: wrap; }
    .aliyah { margin-bottom: 12px; padding: 14px; }
    .line { display: flex; gap: 10px; margin-bottom: 10px; }
  `,
})
export class ParashaEditor {
  readonly id = input<string>();
  private readonly api = inject(Api);
  private readonly router = inject(Router);

  protected readonly name = signal('');
  protected readonly hebrewName = signal('');
  protected readonly intro = signal('');
  protected readonly statut = signal<Statut>('DRAFT');
  protected readonly aliyot = signal<Aliyah[]>([]);
  protected readonly catalog = signal<CatalogEntry[]>([]);
  protected readonly busy = signal(false);
  protected readonly error = signal('');
  private autoHebrew = '';

  constructor() {
    this.api.parashaCatalog().subscribe((c) => this.catalog.set(c));
    effect(() => {
      const id = this.id();
      if (!id) return;
      this.api.adminParasha(Number(id)).subscribe((p) => {
        this.name.set(p.name);
        this.hebrewName.set(p.hebrewName ?? '');
        this.intro.set(p.intro ?? '');
        this.statut.set(p.statut);
        this.aliyot.set(p.aliyot ?? []);
      });
    });
  }

  /**
   * Le nom hébreu se remplit tout seul dès que le nom correspond à une parasha du catalogue (orthographe Hebcal),
   * et suit si on change de parasha. Un nom hébreu saisi à la main n'est jamais écrasé.
   */
  protected onName(value: string) {
    this.name.set(value);
    const known = this.catalog().find((c) => this.norm(c.name) === this.norm(value));
    if (known && (this.hebrewName() === '' || this.hebrewName() === this.autoHebrew)) {
      this.hebrewName.set(known.hebrew);
      this.autoHebrew = known.hebrew;
    }
  }

  /** Compare sans tenir compte de la casse, des espaces ni du type d'apostrophe. */
  private norm(s: string) { return s.trim().toLowerCase().replace(/[\u2018\u2019']/g, ''); }

  protected prefill() {
    this.aliyot.update((current) => [...current, ...DEFAULT_LABELS.slice(current.length).map((label) => ({ label, content: '' }))]);
  }
  protected add() { this.aliyot.update((a) => [...a, { label: '', content: '' }]); }
  protected remove(i: number) { this.aliyot.update((a) => a.filter((_, idx) => idx !== i)); }
  protected setLabel(i: number, v: string) { this.aliyot.update((a) => a.map((x, idx) => (idx === i ? { ...x, label: v } : x))); }
  protected setContent(i: number, v: string) { this.aliyot.update((a) => a.map((x, idx) => (idx === i ? { ...x, content: v } : x))); }

  protected save(e: Event) {
    e.preventDefault();
    this.busy.set(true);
    this.error.set('');
    const input = { name: this.name(), hebrewName: this.hebrewName(), intro: this.intro(), aliyot: this.aliyot(), statut: this.statut() };
    const id = this.id();
    const req = id ? this.api.updateParasha(Number(id), input) : this.api.createParasha(input);
    req.subscribe({
      next: () => this.router.navigateByUrl('/admin'),
      error: (err) => {
        this.busy.set(false);
        this.error.set(err.status === 409 ? 'Cette paracha existe déjà.' : "L'enregistrement a échoué.");
      },
    });
  }
}
