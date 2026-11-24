import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

export interface TagRef { id: number; name: string; slug: string; }
export type Statut = 'DRAFT' | 'PUBLISHED' | 'ARCHIVED';

export interface Article {
  id: number;
  slug: string | null;
  title: string;
  content: string | null;
  excerpt: string;
  description: string | null;
  authorEmail: string | null;
  statut: Statut;
  createdAt: string;
  updatedAt: string;
  publishedAt: string | null;
  tags: TagRef[];
  commentsOpen: boolean;
}

export interface ArticleInput { title: string; slug: string; description: string; content: string; statut: Statut; tagIds: number[]; commentsOpen: boolean; }
export interface Settings { commentsEnabled: boolean; }

export interface Comment {
  id: number;
  authorName: string;
  content: string;
  createdAt: string;
  statut: 'PENDING' | 'APPROVED' | null;
  articleId: number | null;
  articleTitle: string | null;
}

export interface ShabbatStatus {
  closed: boolean;
  closesAt: string | null;
  opensAt: string | null;
  parasha: string | null;
  parashaHebrew: string | null;
  city: string | null;
}

export interface Aliyah { label: string | null; content: string | null; }
export interface Parasha {
  id: number; name: string; slug: string; hebrewName: string | null; book: string;
  intro: string | null; aliyot: Aliyah[] | null; statut: Statut; updatedAt: string;
}
export interface ParashaInput { name: string; hebrewName: string; intro: string; aliyot: Aliyah[]; statut: Statut; }
export interface CatalogEntry { name: string; book: string; position: number; }

export interface CalendarData {
  hebrewDate: string | null;
  hebrewDateHe: string | null;
  parasha: { name: string; slug: string; hebrew: string; torah: string | null; haftarah: string | null; summarySlug: string | null } | null;
  candleLighting: string | null;
  havdalah: string | null;
  city: string | null;
  holidays: { date: string; title: string; hebrew: string | null }[];
}

@Injectable({ providedIn: 'root' })
export class Api {
  private readonly http = inject(HttpClient);

  // Public
  publicArticles(tag?: string, q?: string): Observable<Article[]> {
    let params = new HttpParams();
    if (tag) params = params.set('tag', tag);
    if (q) params = params.set('q', q);
    return this.http.get<Article[]>('/api/public/articles', { params });
  }
  /** `key` : slug ou, pour les anciens liens, identifiant numérique. */
  publicArticle(key: string) { return this.http.get<Article>(`/api/public/articles/${encodeURIComponent(key)}`); }
  related(id: number) { return this.http.get<Article[]>(`/api/public/articles/${id}/related`); }
  publicTags() { return this.http.get<TagRef[]>('/api/public/tags'); }
  comments(articleId: number) { return this.http.get<Comment[]>(`/api/public/articles/${articleId}/comments`); }
  postComment(articleId: number, body: { authorName: string; content: string; website: string }) {
    return this.http.post<void>(`/api/public/articles/${articleId}/comments`, body);
  }
  shabbat() { return this.http.get<ShabbatStatus>('/api/public/shabbat'); }

  calendar() { return this.http.get<CalendarData>('/api/public/calendar'); }
  publicParashot() { return this.http.get<Parasha[]>('/api/public/parashot'); }
  publicParasha(slug: string) { return this.http.get<Parasha>(`/api/public/parashot/${slug}`); }
  parashaCatalog() { return this.http.get<CatalogEntry[]>('/api/public/parashot/catalog'); }

  // Auth
  login(email: string, password: string) { return this.http.post<void>('/api/auth/login', { email, password }); }
  logout() { return this.http.post<void>('/api/auth/logout', {}); }
  me() { return this.http.get('/api/auth/me', { responseType: 'text' }); }

  // Admin
  adminArticles() { return this.http.get<Article[]>('/api/articles'); }
  adminArticle(id: number) { return this.http.get<Article>(`/api/articles/${id}`); }
  createArticle(a: ArticleInput) { return this.http.post<Article>('/api/articles', a); }
  updateArticle(id: number, a: ArticleInput) { return this.http.put<Article>(`/api/articles/${id}`, a); }
  deleteArticle(id: number) { return this.http.delete<void>(`/api/articles/${id}`); }
  createTag(name: string) { return this.http.post<TagRef>('/api/tags', { name }); }
  deleteTag(id: number) { return this.http.delete<void>(`/api/tags/${id}`); }
  adminParashot() { return this.http.get<Parasha[]>('/api/parashot'); }
  adminParasha(id: number) { return this.http.get<Parasha>(`/api/parashot/${id}`); }
  createParasha(p: ParashaInput) { return this.http.post<Parasha>('/api/parashot', p); }
  updateParasha(id: number, p: ParashaInput) { return this.http.put<Parasha>(`/api/parashot/${id}`, p); }
  deleteParasha(id: number) { return this.http.delete<void>(`/api/parashot/${id}`); }
  settings() { return this.http.get<Settings>('/api/settings'); }
  updateSettings(s: Settings) { return this.http.put<Settings>('/api/settings', s); }
  adminComments() { return this.http.get<Comment[]>('/api/comments'); }
  approveComment(id: number) { return this.http.post<void>(`/api/comments/${id}/approve`, {}); }
  deleteComment(id: number) { return this.http.delete<void>(`/api/comments/${id}`); }
}
