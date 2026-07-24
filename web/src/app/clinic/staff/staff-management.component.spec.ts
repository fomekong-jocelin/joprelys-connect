import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import { HospitalOrganizationApiService } from '../hospital-organization/hospital-organization-api.service';
import { RbacApiService } from '../rbac/rbac-api.service';
import { RbacRole } from '../rbac/rbac.models';
import { StaffApiService } from './staff-api.service';
import { StaffManagementComponent } from './staff-management.component';
import { StaffMember } from './staff.models';

describe('StaffManagementComponent', () => {
  let component: StaffManagementComponent;
  let fixture: ComponentFixture<StaffManagementComponent>;
  let mockApi: {
    list: ReturnType<typeof vi.fn>;
    invite: ReturnType<typeof vi.fn>;
    update: ReturnType<typeof vi.fn>;
    toggleStatus: ReturnType<typeof vi.fn>;
    listAssignmentRoles: ReturnType<typeof vi.fn>;
    getAssignments: ReturnType<typeof vi.fn>;
  };
  let mockRbacApi: { listRoles: ReturnType<typeof vi.fn> };
  let mockOrganizationApi: {
    listSpecialtyCatalog: ReturnType<typeof vi.fn>;
    listServiceCatalog: ReturnType<typeof vi.fn>;
    listUnits: ReturnType<typeof vi.fn>;
  };

  const roles: RbacRole[] = [
    {
      id: 'role-doctor', code: 'MEDECIN', name: 'Médecin', systemRole: true,
      assignable: true, enabled: true, permissions: ['CLINICAL_READ'],
    },
    {
      id: 'role-cashier', code: 'CAISSIER', name: 'Caissier', systemRole: true,
      assignable: true, enabled: true, permissions: ['CASH_PAYMENT_COLLECT'],
    },
    {
      id: 'role-daf', code: 'DAF', name: 'Directeur administratif et financier', systemRole: true,
      assignable: true, enabled: true, permissions: ['ACCOUNTING_DASHBOARD_READ'],
    },
    {
      id: 'role-custom', code: 'SUPERVISEUR_CAISSE', name: 'Superviseur caisse', systemRole: false,
      assignable: true, enabled: true, permissions: ['CASH_QUEUE_READ'],
    },
  ];

  const staff: StaffMember[] = [{
    id: 'staff-1', email: 'medecin@joprelys.local', displayName: 'Dr Alpha', role: 'MEDECIN',
    enabled: true, createdAt: '2026-07-02T12:00:00Z', registrationNumber: 'ONMC-001',
  }];

  beforeEach(async () => {
    mockApi = {
      list: vi.fn().mockReturnValue(of(staff)),
      invite: vi.fn(),
      update: vi.fn(),
      toggleStatus: vi.fn(),
      listAssignmentRoles: vi.fn().mockReturnValue(of([
        { code: 'PRACTITIONER', nameFr: 'Praticien', nameEn: 'Practitioner' },
        { code: 'ADMINISTRATIVE_SUPPORT', nameFr: 'Support administratif', nameEn: 'Administrative support' },
      ])),
      getAssignments: vi.fn().mockReturnValue(of({ specialties: [], unitAssignments: [] })),
    };
    mockRbacApi = { listRoles: vi.fn().mockReturnValue(of(roles)) };
    mockOrganizationApi = {
      listSpecialtyCatalog: vi.fn().mockReturnValue(of([
        { code: 'GENERAL_MEDICINE', nameFr: 'Médecine générale', nameEn: 'General medicine' },
      ])),
      listServiceCatalog: vi.fn().mockReturnValue(of([
        { code: 'GENERAL_MEDICINE', nameFr: 'Médecine générale', nameEn: 'General medicine' },
      ])),
      listUnits: vi.fn().mockReturnValue(of([
        {
          id: 'unit-general', parentId: null, code: 'SRV-MG', name: null, unitType: 'SERVICE',
          serviceCatalogCode: 'GENERAL_MEDICINE', active: true,
        },
      ])),
    };

    await TestBed.configureTestingModule({
      imports: [StaffManagementComponent],
      providers: [
        provideRouter([]),
        { provide: StaffApiService, useValue: mockApi },
        { provide: RbacApiService, useValue: mockRbacApi },
        { provide: HospitalOrganizationApiService, useValue: mockOrganizationApi },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(StaffManagementComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should load staff, catalogs, units and assignment roles on init', () => {
    expect(mockApi.list).toHaveBeenCalled();
    expect(mockRbacApi.listRoles).toHaveBeenCalled();
    expect(mockOrganizationApi.listSpecialtyCatalog).toHaveBeenCalled();
    expect(mockOrganizationApi.listUnits).toHaveBeenCalled();
    expect(mockApi.listAssignmentRoles).toHaveBeenCalled();
    expect(component.staff()).toEqual(staff);
    expect(component.roles().map((role) => role.code)).toEqual([
      'CAISSIER', 'DAF', 'MEDECIN', 'SUPERVISEUR_CAISSE',
    ]);
    expect(component.loading()).toBe(false);
  });

  it('should invite a doctor with professional profile and structured primary assignments', () => {
    component.displayName.set('Dr Démo');
    component.email.set('demo@joprelys.local');
    component.selectedRoles.set(['MEDECIN']);
    component.phone.set('+237677889900');
    component.registrationNumber.set('ONMC-DEMO-001');
    component.bio.set('Médecin de démonstration');
    component.initialSpecialtyCode.set('GENERAL_MEDICINE');
    component.initialUnitId.set('unit-general');
    component.initialAssignmentRoleCode.set('PRACTITIONER');
    component.initialAssignmentFrom.set('2026-07-24T08:00');
    mockApi.invite.mockReturnValue(of({
      ...staff[0], id: 'staff-2', email: 'demo@joprelys.local', displayName: 'Dr Démo',
    }));

    component.submitForm();

    expect(mockApi.invite).toHaveBeenCalledWith(expect.objectContaining({
      displayName: 'Dr Démo',
      email: 'demo@joprelys.local',
      roles: ['MEDECIN'],
      phone: '+237677889900',
      registrationNumber: 'ONMC-DEMO-001',
      bio: 'Médecin de démonstration',
      specialtyAssignments: [expect.objectContaining({
        specialtyCode: 'GENERAL_MEDICINE', primary: true,
      })],
      unitAssignments: [expect.objectContaining({
        organizationalUnitId: 'unit-general', assignmentRoleCode: 'PRACTITIONER', primary: true,
      })],
    }));
  });

  it('should require a registration number when a doctor role is selected', () => {
    component.displayName.set('Dr Incomplet');
    component.email.set('incomplete@joprelys.local');
    component.selectedRoles.set(['MEDECIN']);
    component.registrationNumber.set('');

    component.submitForm();

    expect(mockApi.invite).not.toHaveBeenCalled();
    expect(component.formError()).toContain("numéro d'inscription");
  });

  it('should invite staff with multiple system and custom roles', () => {
    component.displayName.set('Responsable Finance');
    component.email.set('finance@joprelys.local');
    component.selectedRoles.set(['DAF', 'CAISSIER', 'SUPERVISEUR_CAISSE']);
    mockApi.invite.mockReturnValue(of({
      ...staff[0], id: 'staff-2', email: 'finance@joprelys.local',
      displayName: 'Responsable Finance', role: 'DAF,CAISSIER,SUPERVISEUR_CAISSE',
    }));

    component.submitForm();

    expect(mockApi.invite).toHaveBeenCalledWith(expect.objectContaining({
      displayName: 'Responsable Finance',
      email: 'finance@joprelys.local',
      roles: ['DAF', 'CAISSIER', 'SUPERVISEUR_CAISSE'],
    }));
  });

  it('should explain when the recipient address is rejected', () => {
    component.displayName.set('Compte Injoignable');
    component.email.set('missing@example.invalid');
    component.selectedRoles.set(['CAISSIER']);
    mockApi.invite.mockReturnValue(throwError(() => ({
      status: 422,
      error: { error: { code: 'MAIL_RECIPIENT_REJECTED', message: 'Recipient rejected', trace_id: 'trc_test' } },
    })));

    component.submitForm();

    expect(component.formError()).toBe(component.t('staff.errors.mailRecipientRejected'));
    expect(component.formLoading()).toBe(false);
    expect(component.staff()).toEqual(staff);
  });

  it('should distinguish a temporary mail outage from an invalid recipient', () => {
    component.displayName.set('Compte à réessayer');
    component.email.set('retry@joprelys.com');
    component.selectedRoles.set(['CAISSIER']);
    mockApi.invite.mockReturnValue(throwError(() => ({
      status: 503,
      error: { error: { code: 'MAIL_DELIVERY_UNAVAILABLE', message: 'Mail service unavailable', trace_id: 'trc_test' } },
    })));

    component.submitForm();

    expect(component.formError()).toBe(component.t('staff.errors.mailDeliveryUnavailable'));
    expect(component.formLoading()).toBe(false);
    expect(component.staff()).toEqual(staff);
  });

  it('should update the effective roles of a selected staff member', () => {
    component.startEdit(staff[0]);
    component.displayName.set('Dr Alpha Senior');
    component.selectedRoles.set(['MEDECIN', 'SUPERVISEUR_CAISSE']);
    component.registrationNumber.set('ONMC-001');
    mockApi.update.mockReturnValue(of({
      ...staff[0], displayName: 'Dr Alpha Senior', role: 'MEDECIN,SUPERVISEUR_CAISSE',
    }));

    component.submitForm();

    expect(mockApi.update).toHaveBeenCalledWith('staff-1', expect.objectContaining({
      displayName: 'Dr Alpha Senior',
      roles: ['MEDECIN', 'SUPERVISEUR_CAISSE'],
    }));
  });

  it('should toggle staff status', () => {
    mockApi.toggleStatus.mockReturnValue(of({ ...staff[0], enabled: false }));
    component.toggleStatus(staff[0]);
    expect(mockApi.toggleStatus).toHaveBeenCalledWith('staff-1');
  });
});
