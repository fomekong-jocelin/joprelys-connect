import { signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import { I18nService } from '../../core/i18n/i18n.service';
import { AvailabilityApiService } from './availability-api.service';
import { AvailabilityPageComponent } from './availability-page.component';
import { AvailabilityException, AvailabilityRule } from './availability.models';
import { toLocalDateKey } from './availability-slots.util';

describe('AvailabilityPageComponent', () => {
  let component: AvailabilityPageComponent;
  let fixture: ComponentFixture<AvailabilityPageComponent>;
  let mockApi: {
    listRules: ReturnType<typeof vi.fn>;
    createRule: ReturnType<typeof vi.fn>;
    updateRule: ReturnType<typeof vi.fn>;
    deactivateRule: ReturnType<typeof vi.fn>;
    listExceptions: ReturnType<typeof vi.fn>;
    createException: ReturnType<typeof vi.fn>;
    deleteException: ReturnType<typeof vi.fn>;
  };
  let mockI18n: { t: ReturnType<typeof vi.fn>; locale: ReturnType<typeof signal<string>> };

  const rules: AvailabilityRule[] = [
    {
      id: 'rule-1',
      doctorId: 'doctor-1',
      weekday: 1,
      startTime: '08:00',
      endTime: '12:00',
      validFrom: '2026-01-01',
      validTo: null,
      active: true,
      createdAt: '2026-07-01T09:00:00Z',
      updatedAt: '2026-07-01T09:00:00Z',
    },
    {
      id: 'rule-2',
      doctorId: 'doctor-1',
      weekday: 3,
      startTime: '14:00',
      endTime: '18:00',
      validFrom: '2026-01-01',
      validTo: '2026-12-31',
      active: false,
      createdAt: '2026-07-01T09:00:00Z',
      updatedAt: '2026-07-01T09:00:00Z',
    },
  ];

  const exceptions: AvailabilityException[] = [
    {
      id: 'exception-1',
      doctorId: 'doctor-1',
      startAt: '2026-07-10T08:00:00Z',
      endAt: '2026-07-10T12:00:00Z',
      reason: 'Congés',
      createdAt: '2026-07-01T09:00:00Z',
      updatedAt: '2026-07-01T09:00:00Z',
    },
  ];

  beforeEach(async () => {
    mockApi = {
      listRules: vi.fn().mockReturnValue(of(rules)),
      createRule: vi.fn(),
      updateRule: vi.fn(),
      deactivateRule: vi.fn(),
      listExceptions: vi.fn().mockReturnValue(of(exceptions)),
      createException: vi.fn(),
      deleteException: vi.fn(),
    };
    mockI18n = {
      t: vi.fn().mockImplementation((key: string) => key),
      locale: signal('fr'),
    };

    await TestBed.configureTestingModule({
      imports: [AvailabilityPageComponent],
      providers: [
        provideRouter([]),
        { provide: AvailabilityApiService, useValue: mockApi },
        { provide: I18nService, useValue: mockI18n },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(AvailabilityPageComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('charge les règles et indisponibilités au démarrage', () => {
    expect(mockApi.listRules).toHaveBeenCalledWith();
    expect(mockApi.listExceptions).toHaveBeenCalled();
    expect(component.rules()).toEqual(rules);
    expect(component.exceptions()).toEqual(exceptions);
    expect(component.loading()).toBe(false);
    expect(component.pageError()).toBeNull();
  });

  it('expose un état d\'erreur quand le chargement échoue', () => {
    mockApi.listRules.mockReturnValueOnce(throwError(() => ({ status: 500 })));
    component.load();
    expect(component.loading()).toBe(false);
    expect(component.pageError()).toBe('common.error.server');
  });

  it('expose un état vide sans règle ni indisponibilité', () => {
    mockApi.listRules.mockReturnValueOnce(of([]));
    mockApi.listExceptions.mockReturnValueOnce(of([]));
    component.load();
    expect(component.rules()).toEqual([]);
    expect(component.sortedExceptions()).toEqual([]);
    expect(component.hasAnySlots()).toBe(false);
  });

  it('soumet une nouvelle plage hebdomadaire', () => {
    mockApi.createRule.mockReturnValue(of(rules[0]));
    component.formWeekday.set(2);
    component.formStartTime.set('09:00');
    component.formEndTime.set('12:00');
    component.formValidFrom.set('2026-07-01');

    component.submitRuleForm();

    expect(mockApi.createRule).toHaveBeenCalledWith({
      weekday: 2,
      startTime: '09:00',
      endTime: '12:00',
      validFrom: '2026-07-01',
      validTo: null,
    });
    expect(component.showRuleForm()).toBe(false);
    expect(component.successMessage()).toBe('availability.success.ruleCreated');
  });

  it('rejette une plage dont la fin précède le début', () => {
    component.formStartTime.set('12:00');
    component.formEndTime.set('09:00');
    component.formValidFrom.set('2026-07-01');

    component.submitRuleForm();

    expect(mockApi.createRule).not.toHaveBeenCalled();
    expect(component.ruleFormError()).toBe('availability.form.invalidTimeRange');
  });

  it('modifie une plage existante', () => {
    component.startEditRule(rules[0]);
    expect(component.editingRule()).toEqual(rules[0]);
    expect(component.showRuleForm()).toBe(true);

    component.formEndTime.set('13:00');
    mockApi.updateRule.mockReturnValue(of({ ...rules[0], endTime: '13:00' }));

    component.submitRuleForm();

    expect(mockApi.updateRule).toHaveBeenCalledWith('rule-1', {
      weekday: 1,
      startTime: '08:00',
      endTime: '13:00',
      validFrom: '2026-01-01',
      validTo: null,
    });
    expect(component.successMessage()).toBe('availability.success.ruleUpdated');
  });

  it('mappe le code métier AVAILABILITY_OVERLAP via i18n', () => {
    mockI18n.t.mockImplementation((key: string) =>
      key === 'availability.error.AVAILABILITY_OVERLAP' ? 'Chevauchement détecté' : key);
    mockApi.createRule.mockReturnValue(throwError(() => ({
      status: 409,
      error: { code: 'AVAILABILITY_OVERLAP', message: 'overlap' },
    })));
    component.formWeekday.set(1);
    component.formStartTime.set('08:00');
    component.formEndTime.set('10:00');
    component.formValidFrom.set('2026-07-01');

    component.submitRuleForm();

    expect(component.ruleFormError()).toBe('Chevauchement détecté');
    expect(component.ruleFormLoading()).toBe(false);
  });

  it('désactive une plage après confirmation', () => {
    mockApi.deactivateRule.mockReturnValue(of({ ...rules[0], active: false }));

    component.askDeactivateRule(rules[0]);
    expect(component.pendingAction()?.kind).toBe('deactivate-rule');

    component.confirmPendingAction();

    expect(mockApi.deactivateRule).toHaveBeenCalledWith('rule-1');
    expect(component.pendingAction()).toBeNull();
    expect(component.successMessage()).toBe('availability.success.ruleDeactivated');
  });

  it('crée une indisponibilité avec conversion en Instant ISO', () => {
    mockApi.createException.mockReturnValue(of(exceptions[0]));
    component.exceptionStart.set('2026-07-10T09:00');
    component.exceptionEnd.set('2026-07-10T12:00');
    component.exceptionReason.set('  Congés  ');

    component.submitExceptionForm();

    expect(mockApi.createException).toHaveBeenCalledWith({
      startAt: new Date('2026-07-10T09:00').toISOString(),
      endAt: new Date('2026-07-10T12:00').toISOString(),
      reason: 'Congés',
    });
    expect(component.successMessage()).toBe('availability.success.exceptionCreated');
  });

  it('rejette une indisponibilité dont la fin précède le début', () => {
    component.exceptionStart.set('2026-07-10T12:00');
    component.exceptionEnd.set('2026-07-10T09:00');

    component.submitExceptionForm();

    expect(mockApi.createException).not.toHaveBeenCalled();
    expect(component.exceptionFormError()).toBe('availability.form.invalidDateRange');
  });

  it('supprime une indisponibilité après confirmation', () => {
    mockApi.deleteException.mockReturnValue(of(void 0));

    component.askDeleteException(exceptions[0]);
    expect(component.pendingAction()?.kind).toBe('delete-exception');

    component.confirmPendingAction();

    expect(mockApi.deleteException).toHaveBeenCalledWith('exception-1');
    expect(component.pendingAction()).toBeNull();
    expect(component.successMessage()).toBe('availability.success.exceptionDeleted');
  });

  it('calcule l\'aperçu des créneaux (règles actives moins exceptions)', () => {
    const tomorrow = new Date();
    tomorrow.setDate(tomorrow.getDate() + 1);
    const isoWeekday = ((tomorrow.getDay() + 6) % 7) + 1;
    const dateKey = toLocalDateKey(tomorrow);
    const rule: AvailabilityRule = {
      id: 'rule-preview',
      doctorId: 'doctor-1',
      weekday: isoWeekday,
      startTime: '09:00',
      endTime: '11:00',
      validFrom: dateKey,
      validTo: null,
      active: true,
      createdAt: '2026-07-01T09:00:00Z',
      updatedAt: '2026-07-01T09:00:00Z',
    };
    component.rules.set([rule, { ...rule, id: 'rule-inactive', active: false }]);
    component.exceptions.set([]);

    const day = component.slotsPreview().find((item) => toLocalDateKey(item.date) === dateKey);
    expect(day).toBeDefined();
    expect(day?.slots.length).toBe(4); // 09:00, 09:30, 10:00, 10:30

    component.exceptions.set([{
      id: 'exception-preview',
      doctorId: 'doctor-1',
      startAt: new Date(tomorrow.getFullYear(), tomorrow.getMonth(), tomorrow.getDate(), 9, 0).toISOString(),
      endAt: new Date(tomorrow.getFullYear(), tomorrow.getMonth(), tomorrow.getDate(), 10, 0).toISOString(),
      reason: null,
      createdAt: '2026-07-01T09:00:00Z',
      updatedAt: '2026-07-01T09:00:00Z',
    }]);

    const dayWithException = component.slotsPreview().find((item) => toLocalDateKey(item.date) === dateKey);
    expect(dayWithException?.slots.length).toBe(2); // 10:00, 10:30
  });
});
