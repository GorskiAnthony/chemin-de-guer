import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

export interface TagRef { id: number; name: string; slug: string; }
export type Statut = 'DRAFT' | 'PUBLISHED' | 'ARCHIVED';

export interface Article {
  id: number;
  title: string;
  content: string | null;
  excerpt: string;
  authorEmail: string | null;
  statut: Statut;
  createdAt: string;
  updatedAt: string;
  publishedAt: string | null;
  tags: TagRef[];
}

export interface ArticleInput { title: string; content: string; statut: Statut; tagIds: number[]; }

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
  publicArticle(id: number) { return this.http.get<Article>(`/api/public/articles/${id}`); }
  related(id: number) { return this.http.get<Article[]>(`/api/public/articles/${id}/related`); }
  publicTags() { return this.http.get<TagRef[]>('/api/public/tags'); }
  comments(articleId: number) { return this.http.get<Comment[]>(`/api/public/articles/${articleId}/comments`); }
  postComment(articleId: number, body: { authorName: string; content: string; website: string }) {
    return this.http.post<void>(`/api/public/articles/${articleId}/comments`, body);
  }
  shabbat() { return this.http.get<ShabbatStatus>('/api/public/shabbat'); }

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
  adminComments() { return this.http.get<Comment[]>('/api/comments'); }
  approveComment(id: number) { return this.http.post<void>(`/api/comments/${id}/approve`, {}); }
  deleteComment(id: number) { return this.http.delete<void>(`/api/comments/${id}`); }
}
