import { DOCUMENT } from '@angular/common';
import { Component, computed, inject, input, signal } from '@angular/core';

/** Boutons de partage : liens directs vers les réseaux (sans script tiers ni pistage), copie du lien, partage natif. */
@Component({
  selector: 'app-share',
  template: `
    <section class="share" aria-labelledby="share-title">
      <h2 id="share-title">Partager</h2>
      <div class="row">
        @if (canNativeShare) {
          <button type="button" class="btn small" (click)="nativeShare()">Partager…</button>
        }
        @for (l of links(); track l.name) {
          <a class="btn small secondary" [href]="l.href" target="_blank" rel="noopener noreferrer"
             [attr.aria-label]="'Partager sur ' + l.name + ' (nouvel onglet)'">{{ l.name }}</a>
        }
        <button type="button" class="btn small ghost" (click)="copy()">{{ copied() ? 'Lien copié ✓' : 'Copier le lien' }}</button>
      </div>
      <p class="sr-only" role="status" aria-live="polite">{{ copied() ? 'Le lien a été copié dans le presse-papiers' : '' }}</p>
    </section>
  `,
  styles: `
    .share { margin-top: 2.5rem; padding-top: 1.5rem; border-top: 1px solid var(--border); }
    .share h2 { font-size: 1.2rem; margin: 0 0 .8rem; }
    .row { display: flex; flex-wrap: wrap; gap: .5rem; }
    .sr-only { position: absolute; width: 1px; height: 1px; overflow: hidden; clip: rect(0 0 0 0); white-space: nowrap; }
  `,
})
export class Share {
  /** Chemin de la page à partager, par exemple /articles/mon-slug */
  readonly path = input.required<string>();
  readonly title = input.required<string>();

  private readonly doc = inject(DOCUMENT);
  protected readonly copied = signal(false);
  protected readonly canNativeShare = typeof navigator !== 'undefined' && typeof navigator.share === 'function';

  private readonly url = computed(() => this.doc.location.origin + this.path());

  protected readonly links = computed(() => {
    const u = encodeURIComponent(this.url());
    const t = encodeURIComponent(this.title());
    return [
      { name: 'WhatsApp', href: `https://wa.me/?text=${t}%20${u}` },
      { name: 'Facebook', href: `https://www.facebook.com/sharer/sharer.php?u=${u}` },
      { name: 'X', href: `https://twitter.com/intent/tweet?text=${t}&url=${u}` },
      { name: 'LinkedIn', href: `https://www.linkedin.com/sharing/share-offsite/?url=${u}` },
      { name: 'Telegram', href: `https://t.me/share/url?url=${u}&text=${t}` },
      { name: 'E-mail', href: `mailto:?subject=${t}&body=${u}` },
    ];
  });

  protected async nativeShare() {
    try {
      await navigator.share({ title: this.title(), url: this.url() });
    } catch {
      /* partage annulé par l'utilisateur */
    }
  }

  protected async copy() {
    try {
      await navigator.clipboard.writeText(this.url());
      this.copied.set(true);
      setTimeout(() => this.copied.set(false), 2500);
    } catch {
      window.prompt('Copiez ce lien :', this.url());
    }
  }
}
