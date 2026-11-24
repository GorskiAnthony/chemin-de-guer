import { DatePipe } from '@angular/common';
import { Component, input } from '@angular/core';
import { ShabbatStatus } from '../core/api';

@Component({
  selector: 'app-shabbat-closed',
  imports: [DatePipe],
  template: `
    <section class="banner">
      <div class="inner">
        <div class="flame"></div>
        <p class="he">שבת שלום</p>
        <h1>Site fermé pour Shabbat</h1>
        <p>Chemin de Guer fait une pause. Le site rouvrira à la sortie de Shabbat
          @if (status().opensAt) { le {{ status().opensAt | date: 'EEEE d MMMM à HH:mm' : undefined : 'fr' }} }.</p>
        @if (status().parasha) { <p class="para">Paracha de la semaine : {{ status().parasha }} · {{ status().parashaHebrew }}</p> }
      </div>
    </section>
  `,
  styles: `
    .banner { position: relative; overflow: hidden; margin-top: 40px; padding: 56px 40px; border-radius: 12px;
      background: var(--panel); color: #f3efe2; text-align: center; }
    .banner::before { content: ""; position: absolute; inset: 0;
      background: repeating-linear-gradient(135deg, rgba(255,255,255,.04) 0 10px, transparent 10px 20px); }
    .inner { position: relative; max-width: 50ch; margin-inline: auto; }
    .flame { width: 14px; height: 20px; margin: 0 auto 16px; background: var(--gold);
      border-radius: 50% 50% 50% 50% / 60% 60% 40% 40%; }
    .he { font: 700 2.2rem var(--serif); color: var(--panel-gold); margin: 0; direction: rtl; }
    h1 { color: #fff; font-size: clamp(1.8rem, 5vw, 2.6rem); margin: 6px 0 14px; }
    p { margin: 0 0 10px; opacity: .9; }
    .para { font: 400 .85rem var(--mono); opacity: .75; margin-top: 22px; }
  `,
})
export class ShabbatClosed {
  readonly status = input.required<ShabbatStatus>();
}
