import { Marked } from 'marked';

function escapeHtml(s: string): string {
  return s.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');
}

/**
 * Rendu Markdown (GFM : listes, citations, séparateurs `---`, tableaux, gras/italique…).
 * - Le HTML brut saisi dans le texte est échappé, jamais interprété.
 * - Les titres sont décalés d'un niveau (# devient h2) car le titre de l'article est déjà le h1 de la page.
 * - Angular assainit en plus le résultat à l'affichage ([innerHTML]).
 */
const md = new Marked({ gfm: true, breaks: true });
md.use({
  renderer: {
    html({ text }) {
      return escapeHtml(text);
    },
    heading({ tokens, depth }) {
      const level = Math.min(depth + 1, 6);
      return `<h${level}>${this.parser.parseInline(tokens)}</h${level}>\n`;
    },
  },
});

export function renderMarkdown(source: string): string {
  return md.parse(source ?? '', { async: false }) as string;
}
