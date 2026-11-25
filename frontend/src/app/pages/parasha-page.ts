import { DatePipe } from '@angular/common';
import { Component, computed, effect, inject, input, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Api, Parasha } from '../core/api';
import { renderMarkdown } from '../core/markdown';
import { Seo } from '../core/seo';
import { SITE } from '../core/site';
import { Share } from './share';

@Component({
  selector: 'app-parasha-page',
  imports: [RouterLink, DatePipe, Share],
  template: `
    <div class="narrow">
      <a class="back" routerLink="/paracha">← Toutes les parashiot</a>
      @if (notFound()) { <p class="card" role="alert">Paracha introuvable.</p> }
      @if (p(); as p) {
        <article class="sheet reveal">
          <header>
            <p class="caption">{{ p.book }}</p>
            <h1>{{ p.name }} @if (p.hebrewName) { <span class="he" lang="he" dir="rtl">{{ p.hebrewName }}</span> }</h1>
            <p class="dates mono">Rédigé le <time [attr.datetime]="p.createdAt">{{ p.createdAt | date: 'd MMMM y' : undefined : 'fr' }}</time>@if (changed()) { <span aria-hidden="true"> · </span>Mis à jour le <time [attr.datetime]="p.updatedAt">{{ p.updatedAt | date: 'd MMMM y' : undefined : 'fr' }}</time> }</p>
          </header>
          @if (p.intro) { <div class="intro" [innerHTML]="intro()"></div> }

          @for (a of p.aliyot ?? []; track $index) {
            <section class="aliyah" [attr.aria-labelledby]="'aliyah-' + $index">
              <span class="num" aria-hidden="true">{{ $index + 1 }}</span>
              <div>
                <h2 [id]="'aliyah-' + $index">{{ a.label || 'Montée ' + ($index + 1) }}</h2>
                <div class="text" [innerHTML]="render(a.content)"></div>
              </div>
            </section>
          }
          <app-share [path]="'/paracha/' + p.slug" [title]="'Paracha ' + p.name" />
        </article>
      }
    </div>
  `,
  styles: `
    .back { display: inline-block; margin: 28px 0 8px; font-size: .92rem; text-decoration: none; color: var(--ink-muted); }
    .dates { margin: 0 0 4px; }
    .aliyah { display: grid; grid-template-columns: 2.6rem 1fr; gap: 16px; margin-top: 26px; padding-top: 22px; border-top: 1px solid var(--border); }
    .num { width: 2.6rem; height: 2.6rem; border-radius: 50%; background: var(--panel); color: var(--panel-gold);
      display: grid; place-items: center; font: 600 1rem var(--mono); }
    .aliyah h2 { margin: 2px 0 8px; font-size: 1.35rem; }
    .aliyah :is(.text) h2 { margin-top: 1.6rem; }
  `,
})
export class ParashaPage {
  readonly slug = input.required<string>();
  private readonly api = inject(Api);
  private readonly seo = inject(Seo);
  protected readonly p = signal<Parasha | null>(null);
  protected readonly notFound = signal(false);
  protected readonly changed = computed(() => {
    const x = this.p();
    return !!x && new Date(x.updatedAt).getTime() - new Date(x.createdAt).getTime() > 86_400_000;
  });
  protected readonly intro = computed(() => renderMarkdown(this.p()?.intro ?? ''));

  constructor() {
    effect(() => {
      this.notFound.set(false);
      this.api.publicParasha(this.slug()).subscribe({
        next: (p) => {
          this.p.set(p);
          const intro = (p.intro ?? '').replace(/[#>*_`\[\]-]/g, '').replace(/\s+/g, ' ').trim();
          this.seo.set({
            title: `Paracha ${p.name} : résumé par montée · ${SITE.name}`,
            description: intro.length > 40 ? intro.slice(0, 157).trim() + (intro.length > 157 ? '…' : '') : `Mon résumé de la paracha ${p.name}, montée par montée.`,
            path: `/paracha/${p.slug}`,
            image: `/og/paracha/${p.slug}.png`,
            type: 'article',
          });
        },
        error: () => { this.notFound.set(true); this.seo.set({ title: `Paracha introuvable · ${SITE.name}`, description: 'Cette paracha n’existe pas.', path: '/paracha', noindex: true }); },
      });
    });
  }

  protected render(content: string | null) { return renderMarkdown(content ?? ''); }
}
