import { Routes } from '@angular/router';
import { LegalDocumentId } from './legal-documents';

const legalRoute = (path: string, legalDocument: LegalDocumentId): Routes[number] => ({
  path,
  loadComponent: () => import('./legal-page.component').then(module => module.LegalPageComponent),
  data: { legalDocument },
});

export const legalRoutes: Routes = [
  {
    path: 'legal/privacy-preferences',
    loadComponent: () => import('./consent-preferences.component').then(module => module.ConsentPreferencesComponent),
  },
  legalRoute('legal/privacy', 'privacy'),
  legalRoute('legal/terms', 'terms'),
  legalRoute('legal/legal-notice', 'legal-notice'),
  legalRoute('legal/cookies', 'cookies'),
  legalRoute('legal/health-data', 'health-data'),
  legalRoute('legal/retention', 'retention'),
  legalRoute('legal/rights', 'rights'),
];
