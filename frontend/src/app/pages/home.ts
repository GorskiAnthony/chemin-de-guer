import { DatePipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Api, Article, CalendarData } from '../core/api';

@Component({
  selector: 'app-home',
  imports: [RouterLink, DatePipe],
  template: `
    <section class="hero">
      <p class="he">גר</p>
      <p class="caption">Journal de conversion</p>
      <h1>Chemin de Guer</h1>
      <p class="sub">Un parcours vers le judaïsme raconté pas à pas : les doutes, les rencontres, les étapes qui comptent.</p>
    </section>

    @if (cal(); as c) {
      <section class="week">
        <div class="today reveal">
          <p class="caption">Aujourd'hui</p>
          @if (c.hebrewDate) {
            <p class="hdate">{{ c.hebrewDate }}</p>
            <p class="hdate-he" lang="he" dir="rtl">{{ c.hebrewDateHe }}</p>
          }
          <p class="greg">{{ now | date: 'EEEE d MMMM y' : undefined : 'fr' }}</p>
        </div>

        @if (c.parasha; as p) {
          <div class="card parasha reveal">
            <p class="caption">Paracha de la semaine</p>
            <h2>{{ p.name }} <span class="he-name" lang="he" dir="rtl">{{ p.hebrew }}</span></h2>
            <dl>
              @if (p.torah) { <div><dt>Torah</dt><dd>{{ p.torah }}</dd></div> }
              @if (p.haftarah) { <div><dt>Haftara</dt><dd>{{ p.haftarah }}</dd></div> }
              @if (c.candleLighting) { <div><dt>Allumage</dt><dd>{{ c.candleLighting | date: 'EEE HH:mm' : undefined : 'fr' }}</dd></div> }
              @if (c.havdalah) { <div><dt>Havdala</dt><dd>{{ c.havdalah | date: 'EEE HH:mm' : undefined : 'fr' }}</dd></div> }
            </dl>
            @if (p.summarySlug) {
              <a class="btn" [routerLink]="['/paracha', p.summarySlug]">Lire mon résumé</a>
            } @else {
              <p class="muted soon">Mon résumé de cette paracha arrive bientôt.</p>
            }
          </div>
        }

        @if (c.holidays.length) {
          <div class="card holidays reveal">
            <p class="caption">Prochaines fêtes</p>
            <ul>
              @for (h of c.holidays; track h.date + h.title) {
                <li><span class="mono">{{ h.date | date: 'd MMM' : undefined : 'fr' }}</span>
                  <span>{{ h.title }}</span>
                  @if (h.hebrew) { <span class="he-sm" lang="he" dir="rtl">{{ h.hebrew }}</span> }</li>
              }
            </ul>
          </div>
        }
      </section>
    }

    <section class="latest">
      <div class="head"><h2>Derniers articles</h2><a routerLink="/articles">Tous les articles →</a></div>
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
        } @empty { <p class="muted">Aucun article pour le moment.</p> }
      </div>
    </section>
  `,
  styles: `
    .hero { text-align: center; padding: 44px 0 30px; }
    .he { font: 400 1.3rem var(--serif); color: var(--gold-ink); margin: 0 0 6px; }
    h1 { font-size: clamp(2.6rem, 7vw, 4.4rem); margin: 10px 0 0; letter-spacing: -.01em; }
    .sub { max-width: 52ch; margin: 16px auto 0; color: var(--ink-muted); font-size: 1.05rem; }

    .week { display: grid; grid-template-columns: 1fr 1.25fr; gap: 20px; margin-bottom: 44px; }
    .today { grid-column: 1; position: relative; overflow: hidden; border-radius: 12px; padding: 30px 28px;
      background: var(--blue); color: #f3efe2; display: flex; flex-direction: column; justify-content: center; }
    .today::before { content: ""; position: absolute; inset: 0;
      background: repeating-linear-gradient(135deg, rgba(255,255,255,.04) 0 10px, transparent 10px 20px); }
    .today > * { position: relative; margin: 0; }
    .today .caption { color: var(--gold-soft); opacity: .85; margin-bottom: 14px; }
    .hdate { font: 700 clamp(1.7rem, 4vw, 2.3rem) var(--serif); color: #fff; line-height: 1.15; }
    .hdate-he { font: 400 1.35rem var(--serif); color: var(--gold-soft); margin-top: 6px; text-align: left; }
    .greg { margin-top: 18px; font: 400 .85rem var(--mono); opacity: .8; }
    .greg::first-letter { text-transform: uppercase; }

    .parasha { grid-column: 2; grid-row: 1 / span 2; display: flex; flex-direction: column; gap: 14px; }
    .parasha h2 { margin: 0; font-size: 2rem; }
    .he-name { font-size: 1.5rem; color: var(--gold-ink); margin-inline-start: .6rem; font-weight: 400; }
    dl { display: grid; grid-template-columns: 1fr 1fr; gap: 12px 18px; margin: 0; }
    dt { font: 500 .7rem var(--mono); letter-spacing: .08em; text-transform: uppercase; color: var(--ink-muted); }
    dd { margin: 2px 0 0; font-weight: 500; }
    .parasha .btn { align-self: flex-start; margin-top: auto; }
    .soon { margin: auto 0 0; font-size: .92rem; }

    .holidays { grid-column: 1; }
    .holidays ul { list-style: none; margin: 10px 0 0; padding: 0; display: grid; gap: 10px; }
    .holidays li { display: flex; gap: 12px; align-items: baseline; flex-wrap: wrap; }
    .holidays .mono { min-width: 3.4rem; }
    .he-sm { color: var(--gold-ink); margin-inline-start: auto; font-family: var(--serif); }
    .caption { margin-bottom: 4px; }

    .head { display: flex; justify-content: space-between; align-items: baseline; gap: 12px; margin-bottom: 18px;
      border-bottom: 1px solid var(--border); padding-bottom: 12px; }
    .head h2 { margin: 0; font-size: 1.7rem; }
    .head a { font-size: .92rem; }
    .grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(260px, 1fr)); gap: 20px; }
    @media (max-width: 760px) {
      .week { grid-template-columns: 1fr; }
      .today, .parasha, .holidays { grid-column: 1; grid-row: auto; }
    }
  `,
})
export class Home {
  private readonly api = inject(Api);
  protected readonly cal = signal<CalendarData | null>(null);
  protected readonly articles = signal<Article[]>([]);
  protected readonly now = new Date();

  constructor() {
    this.api.calendar().subscribe({ next: (c) => this.cal.set(c), error: () => this.cal.set(null) });
    this.api.publicArticles().subscribe({ next: (a) => this.articles.set(a.slice(0, 3)), error: () => {} });
  }
}
