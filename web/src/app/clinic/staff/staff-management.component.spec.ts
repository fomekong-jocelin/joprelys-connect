import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';
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
  };

  const staff: StaffMember[] = [
    {
      id: 'staff-1',
      email: 'medecin@joprelys.local',
      displayName: 'Dr Alpha',
      role: 'MEDECIN',
      enabled: true,
      createdAt: '2026-07-02T12:00:00Z',
    },
  ];

  beforeEach(async () => {
    mockApi = {
      list: vi.fn().mockReturnValue(of(staff)),
      invite: vi.fn(),
      update: vi.fn(),
      toggleStatus: vi.fn(),
    };

    await TestBed.configureTestingModule({
      imports: [StaffManagementComponent],
      providers: [
        provideRouter([]),
        { provide: StaffApiService, useValue: mockApi },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(StaffManagementComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should load staff on init', () => {
    expect(mockApi.list).toHaveBeenCalled();
    expect(component.staff()).toEqual(staff);
    expect(component.loading()).toBe(false);
  });

  it('should invite staff and expose temporary password', () => {
    component.displayName.set('Dr Nouveau');
    component.email.set('nouveau@joprelys.local');
    component.selectedRoles.set(['MEDECIN']);
    mockApi.invite.mockReturnValue(of({
      ...staff[0],
      id: 'staff-2',
      email: 'nouveau@joprelys.local',
      displayName: 'Dr Nouveau',
      temporaryPassword: 'Jop-ABC123',
    }));

    component.submitForm();

    expect(mockApi.invite).toHaveBeenCalledWith({
      displayName: 'Dr Nouveau',
      email: 'nouveau@joprelys.local',
      role: 'MEDECIN',
    });
    expect(component.temporaryPassword()).toBe('Jop-ABC123');
    expect(component.staff().length).toBe(2);
  });

  it('should update selected staff member', () => {
    component.startEdit(staff[0]);
    component.displayName.set('Dr Alpha Senior');
    component.selectedRoles.set(['PHARMACIEN']);
    mockApi.update.mockReturnValue(of({
      ...staff[0],
      displayName: 'Dr Alpha Senior',
      role: 'PHARMACIEN',
    }));

    component.submitForm();

    expect(mockApi.update).toHaveBeenCalledWith('staff-1', {
      displayName: 'Dr Alpha Senior',
      role: 'PHARMACIEN',
    });
    expect(component.staff()[0].role).toBe('PHARMACIEN');
  });

  it('should toggle staff status', () => {
    mockApi.toggleStatus.mockReturnValue(of({ ...staff[0], enabled: false }));

    component.toggleStatus(staff[0]);

    expect(mockApi.toggleStatus).toHaveBeenCalledWith('staff-1');
    expect(component.staff()[0].enabled).toBe(false);
  });
});
