import { DOCUMENT } from '@angular/common';
import { Injectable, inject } from '@angular/core';
import { Meta, Title } from '@angular/platform-browser';
import { SITE } from './site';

export interface PageSeo {
  title: string;
  description: string;
  /** Chemin de la page, par exemple /articles/mon-slug */
  path: string;
  /** Chemin de l'image de partage (par défaut l'image du site) */
  image?: string;
  type?: 'website' | 'article';
  noindex?: boolean;
}

const ROBOTS_INDEX = 'index, follow, max-image-preview:large, max-snippet:-1';

/**
 * Met à jour les balises SEO et de partage lors de la navigation interne.
 * Au premier chargement, le serveur les a déjà injectées dans le HTML (indispensable aux robots
 * qui n'exécutent pas JavaScript) : cette classe les maintient cohérentes ensuite.
 */
@Injectable({ providedIn: 'root' })
export class Seo {
  private readonly titleService = inject(Title);
  private readonly meta = inject(Meta);
  private readonly doc = inject(DOCUMENT);

  set(page: PageSeo): void {
    const origin = this.doc.location.origin;
    const url = origin + page.path;
    const image = origin + (page.image ?? '/og-default.png');
    this.titleService.setTitle(page.title);
    this.meta.updateTag({ name: 'description', content: page.description });
    this.meta.updateTag({ name: 'robots', content: page.noindex ? 'noindex, nofollow' : ROBOTS_INDEX });
    this.meta.updateTag({ property: 'og:title', content: page.title });
    this.meta.updateTag({ property: 'og:description', content: page.description });
    this.meta.updateTag({ property: 'og:url', content: url });
    this.meta.updateTag({ property: 'og:type', content: page.type ?? 'website' });
    this.meta.updateTag({ property: 'og:image', content: image });
    this.meta.updateTag({ property: 'og:site_name', content: SITE.name });
    this.meta.updateTag({ name: 'twitter:card', content: 'summary_large_image' });
    this.meta.updateTag({ name: 'twitter:title', content: page.title });
    this.meta.updateTag({ name: 'twitter:description', content: page.description });
    this.meta.updateTag({ name: 'twitter:image', content: image });
    this.canonical(page.noindex ? null : url);
  }

  noindex(): void {
    this.meta.updateTag({ name: 'robots', content: 'noindex, nofollow, noarchive' });
  }

  private canonical(url: string | null): void {
    let link = this.doc.head.querySelector<HTMLLinkElement>('link[rel="canonical"]');
    if (!url) {
      link?.remove();
      return;
    }
    if (!link) {
      link = this.doc.createElement('link');
      link.setAttribute('rel', 'canonical');
      this.doc.head.appendChild(link);
    }
    link.setAttribute('href', url);
  }
}
