import { DatePipe } from '@angular/common';
import { Component, input } from '@angular/core';
import { ShabbatStatus } from '../core/api';

@Component({
  selector: 'app-shabbat-closed',
  imports: [DatePipe],
  template: `
    <section class="card closed">
      <p class="he">שבת שלום</p>
      <h1>Site fermé pour Shabbat</h1>
      <p>Le site est fermé pendant Shabbat. Il rouvrira à la sortie de Shabbat
        @if (status().opensAt) { , le {{ status().opensAt | date: 'EEEE d MMMM à HH:mm' : undefined : 'fr' }} }.
      </p>
      @if (status().parasha) { <p class="muted">Paracha de la semaine : {{ status().parasha }} {{ status().parashaHebrew }}</p> }
    </section>
  `,
  styles: `
    .closed { text-align: center; padding: 3rem 1.5rem; border-top: 4px solid var(--gold); }
    .he { font: 700 2.4rem "Frank Ruhl Libre", serif; color: var(--gold); margin: 0; }
  `,
})
export class ShabbatClosed {
  readonly status = input.required<ShabbatStatus>();
}
