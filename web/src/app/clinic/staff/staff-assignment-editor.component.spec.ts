import { ComponentFixture, TestBed } from '@angular/core';
import { of } from 'rxjs';
import { HospitalOrganizationApiService } from '../hospital-organization/hospital-organization-api.service';
import { StaffApiService } from './staff-api.service';
import { StaffAssignmentEditorComponent } from './staff-assignment-editor.component';

describe('StaffAssignmentEditorComponent', () => {
  let component: StaffAssignmentEditorComponent;
  let fixture: ComponentFixture<StaffAssignmentEditorComponent>;

  let mockStaffApi: {
    getAssignments: ReturnType<typeof vi.fn>;
    listAssignmentRoles: ReturnType<typeof vi.fn>;
    createSpecialtyAssignment: ReturnType<typeof vi.fn>;
    createUnitAssignment: ReturnType<typeof vi.fn>;
    closeSpecialtyAssignment: ReturnType<typeof vi.fn>;
    closeUnitAssignment: ReturnType<typeof vi.fn>;
  };

  let mockOrganizationApi: {
    listSpecialtyCatalog: ReturnType<typeof vi.fn>;
    listServiceCatalog: ReturnType<typeof vi.fn>;
    listUnits: ReturnType<typeof vi.fn>;
  };

  beforeEach(async () => {
    mockStaffApi = {
      getAssignments: vi.fn().mockReturnValue(of({
        specialties: [
          { id: 'spec-1', specialtyCode: 'GENERAL_MEDICINE', primary: true, validFrom: '2026-07-24T10:00:00Z', active: true },
        ],
        unitAssignments: [
          { id: 'unit-1', organizationalUnitId: 'u-1', assignmentRoleCode: 'PRACTITIONER', primary: true, validFrom: '2026-07-24T10:00:00Z', active: true },
        ],
      })),
      listAssignmentRoles: vi.fn().mockReturnValue(of([
        { code: 'PRACTITIONER', nameFr: 'Praticien', nameEn: 'Practitioner' },
      ])),
      createSpecialtyAssignment: vi.fn().mockReturnValue(of({})),
      createUnitAssignment: vi.fn().mockReturnValue(of({})),
      closeSpecialtyAssignment: vi.fn().mockReturnValue(of({})),
      closeUnitAssignment: vi.fn().mockReturnValue(of({})),
    };

    mockOrganizationApi = {
      listSpecialtyCatalog: vi.fn().mockReturnValue(of([
        { code: 'GENERAL_MEDICINE', nameFr: 'Médecine générale', nameEn: 'General medicine' },
      ])),
      listServiceCatalog: vi.fn().mockReturnValue(of([])),
      listUnits: vi.fn().mockReturnValue(of([
        { id: 'u-1', code: 'SRV-MG', name: 'Médecine générale', serviceCatalogCode: 'GENERAL_MEDICINE', active: true },
      ])),
    };

    await TestBed.configureTestingModule({
      imports: [StaffAssignmentEditorComponent],
      providers: [
        { provide: StaffApiService, useValue: mockStaffApi },
        { provide: HospitalOrganizationApiService, useValue: mockOrganizationApi },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(StaffAssignmentEditorComponent);
    component = fixture.componentInstance;
    fixture.componentRef.setInput('staffId', 'staff-123');
    fixture.detectChanges();
  });

  it('should load initial assignments and keep creation forms collapsed by default', () => {
    expect(mockStaffApi.getAssignments).toHaveBeenCalledWith('staff-123');
    expect(component.structure().specialties.length).toBe(1);
    expect(component.structure().unitAssignments.length).toBe(1);
    expect(component.showSpecialtyForm()).toBe(false);
    expect(component.showUnitForm()).toBe(false);
  });

  it('should toggle specialty and unit creation forms on demand', () => {
    component.toggleSpecialtyForm();
    expect(component.showSpecialtyForm()).toBe(true);

    component.toggleUnitForm();
    expect(component.showUnitForm()).toBe(true);
  });

  it('should add a specialty assignment and collapse form upon success', () => {
    component.toggleSpecialtyForm();
    component.specialtyCode.set('GENERAL_MEDICINE');
    component.specialtyFrom.set('2026-07-24T12:00');

    component.addSpecialty();

    expect(mockStaffApi.createSpecialtyAssignment).toHaveBeenCalledWith('staff-123', expect.objectContaining({
      specialtyCode: 'GENERAL_MEDICINE',
    }));
    expect(component.showSpecialtyForm()).toBe(false);
  });

  it('should add a unit assignment and collapse form upon success', () => {
    component.toggleUnitForm();
    component.unitId.set('u-1');
    component.unitRoleCode.set('PRACTITIONER');
    component.unitFrom.set('2026-07-24T12:00');

    component.addUnit();

    expect(mockStaffApi.createUnitAssignment).toHaveBeenCalledWith('staff-123', expect.objectContaining({
      organizationalUnitId: 'u-1',
      assignmentRoleCode: 'PRACTITIONER',
    }));
    expect(component.showUnitForm()).toBe(false);
  });
});
