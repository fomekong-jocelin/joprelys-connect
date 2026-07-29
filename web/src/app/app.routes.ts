import { Routes } from '@angular/router';
import { roleGuard } from './auth/role.guard';
import { loginGuard } from './auth/login.guard';
import { PROFESSIONAL_ACCESS_POLICIES } from './auth/professional-access-policies';

export const routes: Routes = [
  {
    path: '',
    loadComponent: () => import('./auth/login.component').then((module) => module.LoginComponent),
    canActivate: [loginGuard],
  },
  {
    path: 'dashboard',
    loadComponent: () => import('./clinic/dashboard.component').then((module) => module.DashboardComponent),
    canActivate: [roleGuard],
    data: { allowAnyInternalRole: true },
  },
  {
    path: 'organizations',
    loadComponent: () => import('./clinic/organizations/organization-list.component').then((module) => module.OrganizationListComponent),
    canActivate: [roleGuard],
    data: { expectedPermissions: ['ORGANIZATION_MANAGE'] },
  },
  {
    path: 'patients',
    loadComponent: () => import('./patient/patient-list.component').then((module) => module.PatientListComponent),
    canActivate: [roleGuard],
    data: { expectedPermissions: ['PATIENT_READ'] },
  },
  {
    path: 'patients/:id',
    loadComponent: () => import('./patient/patient-detail.component').then(m => m.PatientDetailComponent),
    canActivate: [roleGuard],
    data: { expectedPermissions: ['PATIENT_READ'] },
    children: [
      { path: '', redirectTo: 'profile', pathMatch: 'full' },
      {
        path: 'profile',
        loadComponent: () => import('./patient/detail/patient-profile-tab.component').then(m => m.PatientProfileTabComponent),
        canActivate: [roleGuard],
        data: { expectedPermissions: ['PATIENT_READ'], breadcrumb: 'breadcrumb.patients.profile' }
      },
      {
        path: 'consultations',
        loadComponent: () => import('./patient/detail/patient-consultations-tab.component').then(m => m.PatientConsultationsTabComponent),
        canActivate: [roleGuard],
        data: { expectedPermissions: ['CLINICAL_READ'], breadcrumb: 'breadcrumb.patients.consultations' }
      },
      {
        path: 'hospitalizations',
        loadComponent: () => import('./patient/detail/patient-hospitalizations-tab.component').then(m => m.PatientHospitalizationsTabComponent),
        canActivate: [roleGuard],
        data: { expectedPermissions: ['HOSPITALIZATION_READ'], breadcrumb: 'breadcrumb.patients.hospitalizations' }
      },
      {
        path: 'lab-orders',
        loadComponent: () => import('./patient/detail/patient-lab-orders-tab.component').then(m => m.PatientLabOrdersTabComponent),
        canActivate: [roleGuard],
        data: { expectedPermissions: ['LAB_ORDER_READ'], breadcrumb: 'breadcrumb.patients.lab-orders' }
      },
      {
        path: 'audit-trail',
        loadComponent: () => import('./patient/detail/patient-audit-trail-tab.component').then(m => m.PatientAuditTrailTabComponent),
        canActivate: [roleGuard],
        data: { expectedPermissions: ['AUDIT_READ'], breadcrumb: 'breadcrumb.patients.audit' }
      }
    ]
  },
  {
    path: 'clinic/staff',
    loadComponent: () => import('./clinic/staff/staff-management.component').then((module) => module.StaffManagementComponent),
    canActivate: [roleGuard],
    data: { expectedPermissions: ['USER_MANAGE'] },
  },
  {
    path: 'clinic/hospital-organization',
    loadComponent: () => import('./clinic/hospital-organization/hospital-organization-page.component').then((module) => module.HospitalOrganizationPageComponent),
    canActivate: [roleGuard],
    data: { expectedPermissions: ['ORGANIZATION_STRUCTURE_MANAGE'] },
  },
  {
    path: 'clinic/availability',
    loadComponent: () => import('./clinic/availability/availability-page.component').then((module) => module.AvailabilityPageComponent),
    canActivate: [roleGuard],
    data: { expectedPermissions: ['AVAILABILITY_MANAGE'] },
  },
  {
    path: 'clinic/appointments',
    loadComponent: () => import('./clinic/appointments/doctor-appointments-page.component').then((module) => module.DoctorAppointmentsPageComponent),
    canActivate: [roleGuard],
    data: { expectedPermissions: ['APPOINTMENT_READ_OWN'], title: 'title.doctor.appointments' },
  },
  {
    path: 'clinic/rbac',
    loadComponent: () => import('./clinic/rbac/rbac-management.component').then((module) => module.RbacManagementComponent),
    canActivate: [roleGuard],
    data: { expectedPermissions: ['RBAC_MANAGE'] },
  },
  {
    path: 'profile',
    loadComponent: () => import('./profile/profile.component').then((m) => m.ProfileComponent),
    canActivate: [roleGuard],
    data: { expectedPermissions: ['STAFF_PROFILE_ACCESS'] },
  },
  {
    path: 'clinic/duplicates',
    loadComponent: () => import('./clinic/duplicates/duplicates-page.component').then((module) => module.DuplicatesPageComponent),
    canActivate: [roleGuard],
    data: { expectedPermissions: ['PATIENT_MERGE'] },
  },
  {
    path: 'clinic/patient-reconciliation',
    loadComponent: () => import('./patient/reconciliation/patient-reconciliation-page.component').then((module) => module.PatientReconciliationPageComponent),
    canActivate: [roleGuard],
    data: { expectedPermissions: ['PATIENT_MERGE'] },
  },
  {
    path: 'clinic/reception',
    loadComponent: () => import('./reception/reception-logs.component').then((m) => m.ReceptionLogsComponent),
    canActivate: [roleGuard],
    data: { expectedPermissions: ['RECEPTION_READ'] },
  },
  {
    path: 'clinic/emergencies',
    loadComponent: () => import('./emergency/emergency-dashboard.component').then((m) => m.EmergencyDashboardComponent),
    canActivate: [roleGuard],
    data: { expectedPermissions: ['EMERGENCY_READ'] },
  },
  {
    path: 'clinic/admissions/pre-registrations',
    loadComponent: () => import('./patient/pre-registrations/pre-registrations-list.component').then(m => m.PreRegistrationsListComponent),
    canActivate: [roleGuard],
    data: { expectedPermissions: ['PATIENT_WRITE'] }
  },
  {
    path: 'clinic/access-request',
    loadComponent: () => import('./clinic/external-access/clinic-access-request.component').then(m => m.ClinicAccessRequestComponent),
    canActivate: [roleGuard],
    data: { expectedPermissions: ['PATIENT_READ', 'CLINICAL_READ', 'LAB_ORDER_READ', 'PHARMACY_PRESCRIPTION_READ'] },
  },
  {
    path: 'clinic/spatial',
    loadComponent: () => import('./clinic/spatial/spatial-management-page.component').then(m => m.SpatialManagementPageComponent),
    canActivate: [roleGuard],
    data: { expectedPermissions: [...PROFESSIONAL_ACCESS_POLICIES.spatial] },
  },
  {
    path: 'clinic/spatial/configuration',
    loadComponent: () => import('./clinic/spatial/spatial-configuration-page.component').then(m => m.SpatialConfigurationPageComponent),
    canActivate: [roleGuard],
    data: { expectedPermissions: ['SPATIAL_CONFIGURATION_MANAGE'] },
  },
  {
    path: 'clinic/billing',
    loadComponent: () => import('./clinic/billing/billing-management-page.component').then(m => m.BillingManagementPageComponent),
    canActivate: [roleGuard],
    data: { expectedPermissions: [...PROFESSIONAL_ACCESS_POLICIES.billingWorkspace] },
  },
  {
    path: 'clinic/cashier',
    loadComponent: () => import('./clinic/billing/billing-cashier-page.component').then(m => m.BillingCashierPageComponent),
    canActivate: [roleGuard],
    data: { expectedPermissions: [...PROFESSIONAL_ACCESS_POLICIES.cashier] },
  },
  {
    path: 'clinic/billing/invoice/:invoiceId',
    loadComponent: () => import('./clinic/billing/billing-management-page.component').then(m => m.BillingManagementPageComponent),
    canActivate: [roleGuard],
    data: { expectedPermissions: ['BILLING_INVOICE_READ'] },
  },
  {
    path: 'clinic/consultation/:visitId',
    loadComponent: () => import('./consultation/consultation.component').then(m => m.ConsultationComponent),
    canActivate: [roleGuard],
    data: { expectedPermissions: ['CLINICAL_WRITE'] },
  },
  {
    path: 'clinic/lab-orders',
    loadComponent: () => import('./clinic/lab/lab-orders-page.component').then(m => m.LabOrdersPageComponent),
    canActivate: [roleGuard],
    data: { expectedPermissions: [...PROFESSIONAL_ACCESS_POLICIES.labQueue] },
  },
  {
    path: 'unauthorized',
    loadComponent: () => import('./auth/unauthorized.component').then((module) => module.UnauthorizedComponent),
  },
  {
    path: 'verify',
    loadComponent: () => import('./consultation/document-search.component').then(m => m.DocumentSearchComponent),
  },
  {
    path: 'verify/:documentId',
    loadComponent: () => import('./consultation/verification.component').then(m => m.VerificationComponent),
  },
  {
    path: 'verify/patient-summary/:documentId',
    loadComponent: () => import('./consultation/verification.component').then(m => m.VerificationComponent),
  },
  {
    path: 'verify/hospitalization/:documentId',
    loadComponent: () => import('./consultation/verification.component').then(m => m.VerificationComponent),
  },
  {
    path: 'pharmacy/prescriptions',
    loadComponent: () => import('./pharmacy/pharmacy-prescription-verify-page.component').then(m => m.PharmacyPrescriptionVerifyPageComponent),
    canActivate: [roleGuard],
    data: { expectedPermissions: ['PHARMACY_PRESCRIPTION_READ'] },
  },
  {
    path: 'pharmacy/stocks',
    loadComponent: () => import('./pharmacy/pharmacy-stocks.component').then(m => m.PharmacyStocksComponent),
    canActivate: [roleGuard],
    data: { expectedPermissions: ['STOCK_READ', 'STOCK_MANAGE'] },
  },
  {
    path: 'forgot-password',
    loadComponent: () => import('./auth/forgot-password.component').then((module) => module.ForgotPasswordComponent),
  },
  {
    path: 'public/register',
    loadComponent: () => import('./patient/self-registration/patient-self-registration.component').then(m => m.PatientSelfRegistrationComponent),
    data: { title: 'title.public.selfRegistration' }
  },
  {
    path: 'patient/login',
    loadComponent: () => import('./auth/login.component').then((module) => module.LoginComponent),
    canActivate: [loginGuard],
    data: { loginMode: 'patient' },
  },
  {
    path: 'patient/dashboard',
    loadComponent: () => import('./patient/portal/patient-dashboard.component').then(m => m.PatientDashboardComponent),
    canActivate: [roleGuard],
    data: { expectedRoles: ['PATIENT'] },
  },
  {
    path: 'patient/appointments',
    loadComponent: () => import('./patient/portal/appointments/patient-appointments-page.component').then(m => m.PatientAppointmentsPageComponent),
    canActivate: [roleGuard],
    data: { expectedRoles: ['PATIENT'], title: 'title.patient.appointments' },
  },
  {
    path: 'patient/profile',
    loadComponent: () => import('./patient/portal/pages/patient-profile-page.component').then(m => m.PatientProfilePageComponent),
    canActivate: [roleGuard],
    data: { expectedRoles: ['PATIENT'], title: 'title.patient.profile' },
  },
  {
    path: 'patient/summary',
    loadComponent: () => import('./patient/portal/pages/patient-summary-page.component').then(m => m.PatientSummaryPageComponent),
    canActivate: [roleGuard],
    data: { expectedRoles: ['PATIENT'], title: 'title.patient.summary' },
  },
  {
    path: 'patient/prescriptions',
    loadComponent: () => import('./patient/portal/pages/patient-prescriptions-page.component').then(m => m.PatientPrescriptionsPageComponent),
    canActivate: [roleGuard],
    data: { expectedRoles: ['PATIENT'], title: 'title.patient.prescriptions' },
  },
  {
    path: 'patient/results',
    loadComponent: () => import('./patient/portal/pages/patient-results-page.component').then(m => m.PatientResultsPageComponent),
    canActivate: [roleGuard],
    data: { expectedRoles: ['PATIENT'], title: 'title.patient.results' },
  },
  {
    path: 'patient/documents',
    loadComponent: () => import('./patient/portal/pages/patient-documents-page.component').then(m => m.PatientDocumentsPageComponent),
    canActivate: [roleGuard],
    data: { expectedRoles: ['PATIENT'], title: 'title.patient.documents' },
  },
  {
    path: 'patient/qr-code',
    loadComponent: () => import('./patient/portal/pages/patient-qr-code-page.component').then(m => m.PatientQrCodePageComponent),
    canActivate: [roleGuard],
    data: { expectedRoles: ['PATIENT'], title: 'title.patient.qrCode' },
  },
  {
    path: 'patient/consents',
    loadComponent: () => import('./patient/portal/pages/patient-consents-page.component').then(m => m.PatientConsentsPageComponent),
    canActivate: [roleGuard],
    data: { expectedRoles: ['PATIENT'], title: 'title.patient.consents' },
  },
  {
    path: 'patient/privacy',
    loadComponent: () => import('./patient/portal/pages/patient-privacy-page.component').then(m => m.PatientPrivacyPageComponent),
    canActivate: [roleGuard],
    data: { expectedRoles: ['PATIENT'], title: 'title.patient.privacy' },
  },
  {
    path: 'patient/audit',
    loadComponent: () => import('./patient/portal/pages/patient-audit-page.component').then(m => m.PatientAuditPageComponent),
    canActivate: [roleGuard],
    data: { expectedRoles: ['PATIENT'], title: 'title.patient.audit' },
  },
  {
    path: 'patient/requests',
    loadComponent: () => import('./patient/portal/pages/patient-requests-page.component').then(m => m.PatientRequestsPageComponent),
    canActivate: [roleGuard],
    data: { expectedRoles: ['PATIENT'], title: 'title.patient.requests' },
  },
  {
    path: 'patient/notifications',
    loadComponent: () => import('./patient/portal/pages/patient-notifications-page.component').then(m => m.PatientNotificationsPageComponent),
    canActivate: [roleGuard],
    data: { expectedRoles: ['PATIENT'], title: 'title.patient.notifications' },
  },
];
