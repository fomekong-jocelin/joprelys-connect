import { signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { I18nService } from '../../core/i18n/i18n.service';
import { DoctorAppointmentsApiService } from './doctor-appointments-api.service';
import { DoctorAppointmentsPageComponent } from './doctor-appointments-page.component';
import { DoctorAppointment } from './doctor-appointments.models';

describe('DoctorAppointmentsPageComponent', () => {
  let fixture: ComponentFixture<DoctorAppointmentsPageComponent>;
  let component: DoctorAppointmentsPageComponent;
  let api: { listOwn: ReturnType<typeof vi.fn> };

  const appointment: DoctorAppointment = {
    id: 'appointment-1',
    patientId: 'patient-1',
    patientDisplayName: 'Patient Alice',
    patientLocalNumber: 'LOCAL-001',
    startAt: '2026-07-15T08:00:00Z',
    endAt: '2026-07-15T08:30:00Z',
    status: 'CONFIRMED',
    reason: 'Suivi',
    visitId: null,
  };

  beforeEach(async () => {
    vi.useFakeTimers();
    vi.setSystemTime(new Date('2026-07-18T12:00:00Z'));
    api = { listOwn: vi.fn().mockReturnValue(of([appointment])) };
    const i18n = {
      t: vi.fn().mockImplementation((key: string) => key),
      locale: signal<'fr' | 'en'>('fr'),
    };

    await TestBed.configureTestingModule({
      imports: [DoctorAppointmentsPageComponent],
      providers: [
        provideRouter([]),
        { provide: DoctorAppointmentsApiService, useValue: api },
        { provide: I18nService, useValue: i18n },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(DoctorAppointmentsPageComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  afterEach(() => {
    fixture.destroy();
    vi.useRealTimers();
  });

  it('charge la semaine courante au démarrage', () => {
    expect(api.listOwn).toHaveBeenCalledTimes(1);
    expect(component.appointments()).toEqual([appointment]);
    expect(component.loading()).toBe(false);
  });

  it('regroupe le rendez-vous dans le jour correspondant', () => {
    const populatedDay = component.days().find((day) => day.appointments.length > 0);

    expect(populatedDay?.appointments).toEqual([appointment]);
    expect(component.hasAppointments()).toBe(true);
  });

  it('recharge immédiatement après navigation vers la semaine suivante', () => {
    const firstFrom = api.listOwn.mock.calls[0][0];

    component.nextWeek();

    expect(api.listOwn).toHaveBeenCalledTimes(2);
    expect(api.listOwn.mock.calls[1][0]).not.toBe(firstFrom);
  });

  it('actualise automatiquement toutes les trente secondes', () => {
    vi.advanceTimersByTime(30_000);

    expect(api.listOwn).toHaveBeenCalledTimes(2);
    expect(component.refreshing()).toBe(false);
  });
});
