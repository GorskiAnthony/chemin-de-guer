import { Routes } from '@angular/router';
import { authGuard } from './core/auth';

export const routes: Routes = [
  { path: '', title: 'Chemin de Guer', loadComponent: () => import('./pages/home').then((m) => m.Home) },
  { path: 'articles/:id', loadComponent: () => import('./pages/article-page').then((m) => m.ArticlePage) },
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
  { path: '**', redirectTo: '' },
];
