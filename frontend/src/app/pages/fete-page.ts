import { DatePipe } from '@angular/common';
import { Component, computed, effect, inject, input, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Api, Fete } from '../core/api';
import { renderMarkdown } from '../core/markdown';
import { Seo } from '../core/seo';
import { SITE } from '../core/site';
import { Share } from './share';

@Component({
  selector: 'app-fete-page',
  imports: [RouterLink, DatePipe, Share],
  template: `
    <div class="narrow">
      <a class="back" routerLink="/fetes">← Toutes les fêtes</a>
      @if (notFound()) { <p class="card" role="alert">Fête introuvable.</p> }
      @if (f(); as f) {
        <article class="sheet reveal">
          <header>
            <p class="caption">{{ f.category }}</p>
            <h1>{{ f.name }} @if (f.hebrewName) { <span class="he" lang="he" dir="rtl">{{ f.hebrewName }}</span> }</h1>
            @if (f.nextDate) {
              <p class="next">
                <span class="mono">Prochaine date</span>
                <time [attr.datetime]="f.nextDate">{{ f.nextDate | date: 'EEEE d MMMM y' : undefined : 'fr' }}</time>
                @if (daysLabel(); as d) { <span class="muted">· {{ d }}</span> }
              </p>
            }
          </header>
          @if (f.intro) { <div class="intro" [innerHTML]="intro()"></div> }

          @for (s of f.sections ?? []; track $index) {
            <section class="part" [attr.aria-labelledby]="'part-' + $index">
              <h2 [id]="'part-' + $index">{{ s.label || 'Partie ' + ($index + 1) }}</h2>
              <div class="text" [innerHTML]="render(s.content)"></div>
            </section>
          }
          <app-share [path]="'/fetes/' + f.slug" [title]="f.name" />
        </article>
      }
    </div>
  `,
  styles: `
    .back { display: inline-block; margin: 28px 0 8px; font-size: .92rem; text-decoration: none; color: var(--ink-muted); }
    .next { margin: 0 0 18px; display: flex; flex-wrap: wrap; gap: .5rem; align-items: baseline; }
    .next time { font-weight: 600; }
    .next time::first-letter { text-transform: uppercase; }
    .part { margin-top: 28px; padding-top: 22px; border-top: 1px solid var(--border); }
    .part h2 { margin: 0 0 10px; font-size: 1.4rem; padding-inline-start: 14px; border-inline-start: 3px solid var(--gold); }
  `,
})
export class FetePage {
  readonly slug = input.required<string>();
  private readonly api = inject(Api);
  private readonly seo = inject(Seo);
  protected readonly f = signal<Fete | null>(null);
  protected readonly notFound = signal(false);
  protected readonly intro = computed(() => renderMarkdown(this.f()?.intro ?? ''));
  protected readonly daysLabel = computed(() => {
    const next = this.f()?.nextDate;
    if (!next) return '';
    const today = new Date(new Date().toDateString()).getTime();
    const days = Math.round((new Date(next + 'T00:00:00').getTime() - today) / 86_400_000);
    return days === 0 ? "aujourd'hui" : days === 1 ? 'demain' : days > 1 ? `dans ${days} jours` : '';
  });

  constructor() {
    effect(() => {
      this.notFound.set(false);
      this.api.publicFete(this.slug()).subscribe({
        next: (f) => {
          this.f.set(f);
          const intro = (f.intro ?? '').replace(/[#>*_`\[\]-]/g, '').replace(/\s+/g, ' ').trim();
          this.seo.set({
            title: `${f.name} : résumé de la fête · ${SITE.name}`,
            description: intro.length > 40 ? intro.slice(0, 157).trim() + (intro.length > 157 ? '…' : '') : `Mon résumé de la fête de ${f.name} : histoire, coutumes et ce que j'en ai appris.`,
            path: `/fetes/${f.slug}`,
            image: `/og/fetes/${f.slug}.png`,
            type: 'article',
          });
        },
        error: () => {
          this.notFound.set(true);
          this.seo.set({ title: `Fête introuvable · ${SITE.name}`, description: 'Cette fête n’existe pas.', path: '/fetes', noindex: true });
        },
      });
    });
  }

  protected render(content: string | null) { return renderMarkdown(content ?? ''); }
}
