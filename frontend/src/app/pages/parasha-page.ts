import { Component, computed, effect, inject, input, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Api, Parasha } from '../core/api';
import { renderMarkdown } from '../core/markdown';
import { Seo } from '../core/seo';
import { SITE } from '../core/site';
import { Share } from './share';

@Component({
  selector: 'app-parasha-page',
  imports: [RouterLink, Share],
  template: `
    <div class="narrow">
      <a class="back" routerLink="/paracha">← Toutes les parashiot</a>
      @if (notFound()) { <p class="card">Paracha introuvable.</p> }
      @if (p(); as p) {
        <header class="reveal">
          <p class="caption">{{ p.book }}</p>
          <h1>{{ p.name }} @if (p.hebrewName) { <span class="he" lang="he" dir="rtl">{{ p.hebrewName }}</span> }</h1>
        </header>
        @if (p.intro) { <div class="intro" [innerHTML]="intro()"></div> }

        @for (a of p.aliyot ?? []; track $index) {
          <section class="aliyah reveal">
            <span class="num">{{ $index + 1 }}</span>
            <div>
              <h2>{{ a.label || 'Montée ' + ($index + 1) }}</h2>
              <div class="text" [innerHTML]="render(a.content)"></div>
            </div>
          </section>
        }
        <app-share [path]="'/paracha/' + p.slug" [title]="'Paracha ' + p.name" />
      }
    </div>
  `,
  styles: `
    .back { display: inline-block; margin: 28px 0 8px; font-size: .92rem; text-decoration: none; color: var(--ink-muted); }
    h1 { font-size: clamp(2rem, 5vw, 3.1rem); margin: 6px 0 18px; }
    .he { color: var(--gold-ink); font-weight: 400; margin-inline-start: .6rem; }
    .intro { font-size: 1.12rem; line-height: 1.75; padding-bottom: 8px; }
    .aliyah { display: grid; grid-template-columns: 2.6rem 1fr; gap: 16px; margin-top: 26px; padding-top: 22px; border-top: 1px solid var(--border); }
    .num { width: 2.6rem; height: 2.6rem; border-radius: 50%; background: var(--gold-soft); color: var(--gold-ink);
      display: grid; place-items: center; font: 600 1rem var(--mono); }
    .aliyah h2 { margin: 2px 0 8px; font-size: 1.35rem; }
    .text { line-height: 1.7; }
    :host ::ng-deep :is(.intro, .text) blockquote { border-inline-start: 3px solid var(--gold); margin-inline: 0; padding-inline-start: 1rem; color: var(--ink-muted); }
    :host ::ng-deep :is(.intro, .text) p { margin: 0 0 1em; }
  `,
})
export class ParashaPage {
  readonly slug = input.required<string>();
  private readonly api = inject(Api);
  private readonly seo = inject(Seo);
  protected readonly p = signal<Parasha | null>(null);
  protected readonly notFound = signal(false);
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
