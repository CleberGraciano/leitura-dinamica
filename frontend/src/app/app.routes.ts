import { Routes } from '@angular/router';

import { adminGuard, authGuard, guestGuard } from './core/auth.guard';

export const routes: Routes = [
	{
		path: '',
		pathMatch: 'full',
		redirectTo: 'auth'
	},
	{
		path: 'auth',
		canActivate: [guestGuard],
		loadComponent: () => import('./pages/auth-page.component').then((m) => m.AuthPageComponent)
	},
	{
		path: 'app',
		canActivate: [authGuard],
		loadComponent: () => import('./shell/app-shell.component').then((m) => m.AppShellComponent),
		children: [
			{
				path: '',
				pathMatch: 'full',
				redirectTo: 'dashboard'
			},
			{
				path: 'dashboard',
				loadComponent: () => import('./pages/dashboard-page.component').then((m) => m.DashboardPageComponent)
			},
			{
				path: 'library',
				loadComponent: () => import('./pages/library-page.component').then((m) => m.LibraryPageComponent)
			},
			{
				path: 'reader',
				loadComponent: () => import('./pages/reader-page.component').then((m) => m.ReaderPageComponent)
			},
			{
				path: 'training',
				loadComponent: () => import('./pages/training-page.component').then((m) => m.TrainingPageComponent)
			},
			{
				path: 'admin',
				canActivate: [adminGuard],
				loadComponent: () => import('./pages/admin-page.component').then((m) => m.AdminPageComponent)
			}
		]
	},
	{
		path: '**',
		redirectTo: 'auth'
	}
];
