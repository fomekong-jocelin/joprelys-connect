import { Routes } from '@angular/router';
import { roleGuard } from './auth/role.guard';

export const routes: Routes = [
  {
    path: '',
    loadComponent: () => import('./auth/login.component').then((module) => module.LoginComponent),
  },
  {
    path: 'dashboard',
    loadComponent: () => import('./clinic/dashboard.component').then((module) => module.DashboardComponent),
    canActivate: [roleGuard],
    data: { expectedRoles: ['ADMIN_JOPRELYS', 'ADMIN_CLINIQUE', 'AGENT_ACCUEIL', 'INFIRMIER', 'MEDECIN', 'PHARMACIEN'] },
  },
  {
    path: 'organizations',
    loadComponent: () => import('./clinic/organizations/organization-list.component').then((module) => module.OrganizationListComponent),
    canActivate: [roleGuard],
    data: { expectedRoles: ['ADMIN_JOPRELYS'] },
  },
  {
    path: 'patients',
    loadComponent: () => import('./patient/patient-list.component').then((module) => module.PatientListComponent),
    canActivate: [roleGuard],
    data: { expectedRoles: ['AGENT_ACCUEIL', 'INFIRMIER', 'MEDECIN', 'ADMIN_CLINIQUE'] },
  },
  {
    path: 'clinic/staff',
    loadComponent: () => import('./clinic/staff/staff-management.component').then((module) => module.StaffManagementComponent),
    canActivate: [roleGuard],
    data: { expectedRoles: ['ADMIN_CLINIQUE'] },
  },
  {
    path: 'clinic/consultation/:visitId',
    loadComponent: () => import('./consultation/consultation.component').then(m => m.ConsultationComponent),
    canActivate: [roleGuard],
    data: { expectedRoles: ['MEDECIN', 'ADMIN_CLINIQUE'] },
  },
  {
    path: 'clinic/lab-orders',
    loadComponent: () => import('./clinic/lab/lab-orders-page.component').then(m => m.LabOrdersPageComponent),
    canActivate: [roleGuard],
    data: { expectedRoles: ['BIOLOGISTE', 'ADMIN_JOPRELYS'] },
  },
  {
    path: 'unauthorized',
    loadComponent: () => import('./auth/unauthorized.component').then((module) => module.UnauthorizedComponent),
  },
  {
    path: 'verify/:documentId',
    loadComponent: () => import('./consultation/verification.component').then(m => m.VerificationComponent),
  },
  {
    path: 'pharmacy/prescriptions',
    loadComponent: () => import('./pharmacy/pharmacy-prescription-verify-page.component').then(m => m.PharmacyPrescriptionVerifyPageComponent),
    canActivate: [roleGuard],
    data: { expectedRoles: ['PHARMACIEN', 'ADMIN_JOPRELYS'] },
  },
  {
    path: 'pharmacy/stocks',
    loadComponent: () => import('./pharmacy/pharmacy-stocks.component').then(m => m.PharmacyStocksComponent),
    canActivate: [roleGuard],
    data: { expectedRoles: ['PHARMACIEN', 'ADMIN_CLINIQUE'] },
  },
  {
    path: 'forgot-password',
    loadComponent: () => import('./auth/forgot-password.component').then((module) => module.ForgotPasswordComponent),
  },
  {
    path: 'patient/login',
    redirectTo: '',
    pathMatch: 'full',
  },
  {
    path: 'patient/dashboard',
    loadComponent: () => import('./patient/portal/patient-dashboard.component').then(m => m.PatientDashboardComponent),
    canActivate: [roleGuard],
    data: { expectedRoles: ['PATIENT'] },
  },
];
