import { Routes } from '@angular/router';

export const routes: Routes = [
  {
    path: '', // Loads Home at the base URL
    loadComponent: () =>
      import('./features/home/home') // Update to your actual Home component path
        .then(m => m.Home),
  },
  {
    path: 'activate',
    loadComponent: () =>
      import('./features/auth/activation/activation')
        .then(m => m.Activation),
  },
  {
    path: 'dashboard', // Loads Home at the base URL
    loadComponent: () =>
      import('./features/dashboard/dashboard') // Update to your actual Home component path
        .then(m => m.Dashboard),
  },
  { path: '**', redirectTo: '' },
];
