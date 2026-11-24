import { Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Seo } from '../core/seo';

@Component({
  selector: 'app-not-found',
  imports: [RouterLink],
  template: `
    <section class="nf">
      <p class="caption">Erreur 404</p>
      <h1>Page introuvable</h1>
      <p class="muted">Cette adresse n'existe pas ou n'existe plus.</p>
      <p><a class="btn" routerLink="/">Retour à l'accueil</a> <a class="btn secondary" routerLink="/articles">Voir les articles</a></p>
    </section>
  `,
  styles: `.nf { text-align: center; padding: 64px 0; } h1 { margin: 8px 0 12px; font-size: clamp(2rem, 6vw, 3rem); } .btn { margin: 4px; }`,
})
export class NotFound {
  constructor() {
    inject(Seo).set({ title: 'Page introuvable · Chemin de Guer', description: 'Cette page n’existe pas.', path: '/', noindex: true });
  }
}
