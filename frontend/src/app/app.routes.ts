import { Routes } from '@angular/router';
import { authGuard } from './core/auth';

export const routes: Routes = [
  { path: '', title: 'Chemin de Guer', loadComponent: () => import('./pages/home').then((m) => m.Home) },
  { path: 'articles', title: 'Articles', loadComponent: () => import('./pages/articles').then((m) => m.Articles) },
  { path: 'articles/:slug', loadComponent: () => import('./pages/article-page').then((m) => m.ArticlePage) },
  { path: 'paracha', title: 'Paracha', loadComponent: () => import('./pages/parasha-list').then((m) => m.ParashaList) },
  { path: 'paracha/:slug', loadComponent: () => import('./pages/parasha-page').then((m) => m.ParashaPage) },
  { path: 'admin/login', title: 'Connexion', loadComponent: () => import('./admin/login').then((m) => m.Login) },
  {
    path: 'admin',
    canActivate: [authGuard],
    title: 'Administration',
    loadComponent: () => import('./admin/dashboard').then((m) => m.Dashboard),
  },
  {
    path: 'admin/articles/nouveau',
    canActivate: [authGuard],
    title: 'Nouvel article',
    loadComponent: () => import('./admin/article-editor').then((m) => m.ArticleEditor),
  },
  {
    path: 'admin/articles/:id',
    canActivate: [authGuard],
    title: 'Modifier un article',
    loadComponent: () => import('./admin/article-editor').then((m) => m.ArticleEditor),
  },
  {
    path: 'admin/parashot/nouveau',
    canActivate: [authGuard],
    title: 'Nouvelle paracha',
    loadComponent: () => import('./admin/parasha-editor').then((m) => m.ParashaEditor),
  },
  {
    path: 'admin/parashot/:id',
    canActivate: [authGuard],
    title: 'Modifier une paracha',
    loadComponent: () => import('./admin/parasha-editor').then((m) => m.ParashaEditor),
  },
  { path: '**', title: 'Page introuvable', loadComponent: () => import('./pages/not-found').then((m) => m.NotFound) },
];
