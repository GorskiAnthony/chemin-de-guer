import { DatePipe, DecimalPipe } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Api, Stats } from '../core/api';

@Component({
  selector: 'app-stats',
  imports: [RouterLink, DatePipe, DecimalPipe],
  template: `
    <a class="back" routerLink="/admin">← Administration</a>
    <h1>Statistiques</h1>
    <p class="muted">Mesure d'audience anonyme, sans cookie : aucune adresse IP n'est conservée, et vos propres visites connecté ne sont pas comptées.</p>

    <div class="filters" role="group" aria-label="Période">
      @for (p of periods; track p.days) {
        <button type="button" class="tag" [class.on]="days() === p.days" [attr.aria-pressed]="days() === p.days" (click)="load(p.days)">{{ p.label }}</button>
      }
    </div>

    @if (stats(); as s) {
      <div class="kpis">
        <div class="card kpi"><span class="caption">Pages vues</span><strong>{{ s.views | number: '1.0-0' : 'fr' }}</strong></div>
        <div class="card kpi"><span class="caption">Visites</span><strong>{{ s.visits | number: '1.0-0' : 'fr' }}</strong>
          <span class="muted note">visiteurs distincts, comptés par jour</span></div>
        <div class="card kpi"><span class="caption">Moyenne par jour</span><strong>{{ s.views / s.days | number: '1.0-1' : 'fr' }}</strong></div>
      </div>

      <h2>Pages vues par jour</h2>
      <div class="chart" role="img" [attr.aria-label]="chartLabel()">
        @for (d of s.perDay; track d.day) {
          <div class="bar" [title]="(d.day | date: 'd MMM' : undefined : 'fr') + ' : ' + d.views + ' vue(s)'">
            <span [style.height.%]="d.views / max() * 100"></span>
          </div>
        }
      </div>
      <details>
        <summary>Voir les chiffres jour par jour</summary>
        <div class="scroll">
          <table>
            <thead><tr><th scope="col">Jour</th><th scope="col">Pages vues</th><th scope="col">Visiteurs</th></tr></thead>
            <tbody>
              @for (d of reversed(); track d.day) {
                <tr><th scope="row">{{ d.day | date: 'EEE d MMM y' : undefined : 'fr' }}</th><td>{{ d.views }}</td><td>{{ d.visitors }}</td></tr>
              }
            </tbody>
          </table>
        </div>
      </details>

      <div class="cols">
        <section>
          <h2>Pages les plus lues</h2>
          @for (r of s.pages; track r.label) {
            <p class="line"><span class="mono">{{ r.label }}</span><strong>{{ r.views }}</strong></p>
          } @empty { <p class="muted">Aucune visite sur la période.</p> }
        </section>
        <section>
          <h2>D'où viennent les visiteurs</h2>
          @for (r of s.referrers; track r.label) {
            <p class="line"><span class="mono">{{ r.label }}</span><strong>{{ r.views }}</strong></p>
          } @empty { <p class="muted">Aucun site d'origine : accès directs, messageries et applications.</p> }
          <h2>Appareils</h2>
          @for (r of s.devices; track r.label) {
            <p class="line"><span>{{ r.label }}</span><strong>{{ r.views }}</strong></p>
          }
        </section>
      </div>
    } @else if (failed()) {
      <p class="error" role="alert">Impossible de charger les statistiques.</p>
    }
  `,
  styles: `
    .back { display: inline-block; margin: 24px 0 4px; font-size: .92rem; text-decoration: none; color: var(--ink-muted); }
    h2 { margin-top: 2.2rem; font-size: 1.3rem; }
    .filters { margin: 18px 0; }
    button.tag { border: 0; cursor: pointer; font-family: var(--mono); }
    .kpis { display: grid; grid-template-columns: repeat(auto-fit, minmax(180px, 1fr)); gap: 14px; }
    .kpi { display: flex; flex-direction: column; gap: 4px; }
    .kpi strong { font: 600 2.2rem var(--serif); color: var(--blue-strong); }
    .note { font-size: .78rem; }
    .chart { display: flex; align-items: flex-end; gap: 2px; height: 160px; padding: 12px; background: var(--surface);
      border: 1px solid var(--border); border-radius: 10px; }
    .bar { flex: 1; height: 100%; display: flex; align-items: flex-end; min-width: 1px; }
    .bar span { display: block; width: 100%; min-height: 2px; background: var(--blue); border-radius: 2px 2px 0 0; }
    details { margin-top: 12px; }
    summary { cursor: pointer; color: var(--blue); }
    .scroll { overflow-x: auto; margin-top: 10px; }
    table { border-collapse: collapse; min-width: 320px; }
    th, td { text-align: left; padding: 6px 18px 6px 0; border-bottom: 1px solid var(--border); font-size: .92rem; font-weight: 400; }
    thead th { font-weight: 600; }
    tbody th { font-weight: 500; }
    .cols { display: grid; grid-template-columns: repeat(auto-fit, minmax(280px, 1fr)); gap: 0 32px; }
    .line { display: flex; justify-content: space-between; gap: 12px; margin: 0; padding: 7px 0; border-bottom: 1px solid var(--border); overflow-wrap: anywhere; }
  `,
})
export class StatsPage {
  private readonly api = inject(Api);
  protected readonly periods = [
    { days: 7, label: '7 jours' }, { days: 30, label: '30 jours' }, { days: 90, label: '90 jours' }, { days: 365, label: '1 an' },
  ];
  protected readonly days = signal(30);
  protected readonly stats = signal<Stats | null>(null);
  protected readonly failed = signal(false);
  protected readonly max = computed(() => Math.max(1, ...(this.stats()?.perDay.map((d) => d.views) ?? [1])));
  protected readonly reversed = computed(() => [...(this.stats()?.perDay ?? [])].reverse());
  protected readonly chartLabel = computed(() => {
    const s = this.stats();
    return s ? `Graphique des pages vues par jour sur ${s.days} jours : ${s.views} au total. Le détail est dans le tableau ci-dessous.` : '';
  });

  constructor() { this.load(30); }

  protected load(days: number) {
    this.days.set(days);
    this.failed.set(false);
    this.api.stats(days).subscribe({ next: (s) => this.stats.set(s), error: () => this.failed.set(true) });
  }
}
