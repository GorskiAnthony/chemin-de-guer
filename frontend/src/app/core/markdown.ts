/**
 * Rendu Markdown minimal et sûr : le HTML est échappé en premier, seules des balises
 * connues sont ensuite générées. Les liens sont limités à http(s) et aux chemins relatifs.
 */
function escapeHtml(s: string): string {
  return s.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');
}

function inline(s: string): string {
  return s
    .replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>')
    .replace(/(^|[^*])\*(?!\s)(.+?)\*/g, '$1<em>$2</em>')
    .replace(/\[([^\]]+)\]\(((?:https?:\/\/|\/)[^\s)]+)\)/g,
      '<a href="$2" rel="noopener noreferrer" target="_blank">$1</a>');
}

export function renderMarkdown(source: string): string {
  const blocks = escapeHtml(source).split(/\n{2,}/);
  return blocks.map((block) => {
    const b = block.trim();
    if (!b) return '';
    const heading = /^(#{1,3})\s+(.+)$/.exec(b);
    if (heading) {
      const level = heading[1].length + 1;
      return `<h${level}>${inline(heading[2])}</h${level}>`;
    }
    const lines = b.split('\n');
    if (lines.every((l) => /^[-*]\s+/.test(l))) {
      return '<ul>' + lines.map((l) => `<li>${inline(l.replace(/^[-*]\s+/, ''))}</li>`).join('') + '</ul>';
    }
    if (lines.every((l) => /^&gt;\s?/.test(l))) {
      return '<blockquote>' + inline(lines.map((l) => l.replace(/^&gt;\s?/, '')).join('<br>')) + '</blockquote>';
    }
    return '<p>' + inline(lines.join('<br>')) + '</p>';
  }).join('\n');
}
