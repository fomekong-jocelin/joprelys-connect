import { signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import { I18nService } from '../../../core/i18n/i18n.service';
import { PatientAppointmentsApiService } from './patient-appointments-api.service';
import { PatientAppointmentsPageComponent } from './patient-appointments-page.component';
import { AppointmentSlot, DoctorDirectoryEntry, PatientAppointment } from './patient-appointments.models';

describe('PatientAppointmentsPageComponent', () => {
  let fixture: ComponentFixture<PatientAppointmentsPageComponent>;
  let component: PatientAppointmentsPageComponent;
  let api: {
    listDoctors: ReturnType<typeof vi.fn>;
    listSlots: ReturnType<typeof vi.fn>;
    book: ReturnType<typeof vi.fn>;
    listOwn: ReturnType<typeof vi.fn>;
    cancel: ReturnType<typeof vi.fn>;
  };

  const doctor: DoctorDirectoryEntry = {
    doctorId: 'doctor-1',
    displayName: 'Dr Alice',
    specialties: [
      {
        code: 'CARDIOLOGY',
        nameFr: 'Cardiologie',
        nameEn: 'Cardiology',
        primary: true,
      },
    ],
    units: [
      {
        id: 'unit-cardio',
        code: 'CARDIO',
        nameFr: 'Cardiologie',
        nameEn: 'Cardiology',
        primary: true,
      },
    ],
  };

  const slot: AppointmentSlot = {
    doctorId: doctor.doctorId,
    startAt: '2026-08-03T07:00:00Z',
    endAt: '2026-08-03T07:30:00Z',
  };

  const appointment: PatientAppointment = {
    id: 'appointment-1',
    doctorId: doctor.doctorId,
    doctorDisplayName: doctor.displayName,
    patientId: 'patient-1',
    startAt: slot.startAt,
    endAt: slot.endAt,
    status: 'CONFIRMED',
    reason: null,
    cancelledAt: null,
    cancellationReason: null,
    visitId: null,
    reminderSentAt: null,
    createdAt: '2026-07-18T10:00:00Z',
    updatedAt: '2026-07-18T10:00:00Z',
  };

  beforeEach(async () => {
    api = {
      listDoctors: vi.fn().mockReturnValue(of([doctor])),
      listSlots: vi.fn().mockReturnValue(of([slot])),
      book: vi.fn().mockReturnValue(of(appointment)),
      listOwn: vi.fn().mockReturnValue(of([appointment])),
      cancel: vi.fn().mockReturnValue(of({ ...appointment, status: 'CANCELLED_BY_PATIENT' })),
    };
    const locale = signal<'fr' | 'en'>('fr');
    const i18n = {
      t: vi.fn().mockImplementation((key: string, fallback?: string) => fallback ?? key),
      locale,
      currentLanguage: vi.fn().mockImplementation(() => locale()),
    };

    await TestBed.configureTestingModule({
      imports: [PatientAppointmentsPageComponent],
      providers: [
        provideRouter([]),
        { provide: PatientAppointmentsApiService, useValue: api },
        { provide: I18nService, useValue: i18n },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(PatientAppointmentsPageComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('charge les médecins structurés et les rendez-vous au démarrage', () => {
    expect(api.listDoctors).toHaveBeenCalledWith();
    expect(api.listOwn).toHaveBeenCalled();
    expect(component.doctors()).toEqual([doctor]);
    expect(component.appointments()).toEqual([appointment]);
    expect(component.specialtyOptions().map((item) => item.code)).toEqual(['CARDIOLOGY']);
    expect(component.unitOptions().map((item) => item.id)).toEqual(['unit-cardio']);
  });

  it('filtre l’annuaire par code spécialité et UUID unité sans texte libre', () => {
    component.specialtyCodeFilter = 'CARDIOLOGY';
    component.organizationalUnitIdFilter = 'unit-cardio';
    component.onFilterChanged();

    expect(component.doctors()).toEqual([doctor]);

    component.organizationalUnitIdFilter = 'unit-other';
    component.onFilterChanged();
    expect(component.doctors()).toEqual([]);
  });

  it('charge les créneaux après sélection du médecin', () => {
    component.selectDoctor(doctor);
    expect(api.listSlots).toHaveBeenCalled();
    expect(component.slots()).toEqual([slot]);
  });

  it('réserve le créneau sélectionné puis actualise les données', () => {
    component.selectDoctor(doctor);
    component.selectedSlot.set(slot);
    component.reason = '  Contrôle  ';

    component.bookSelectedSlot();

    expect(api.book).toHaveBeenCalledWith({
      doctorId: doctor.doctorId,
      startAt: slot.startAt,
      reason: 'Contrôle',
    });
    expect(component.selectedSlot()).toBeNull();
    expect(component.notice()).toBe('appointments.success.booked');
  });

  it('rafraîchit les créneaux après un conflit SLOT_UNAVAILABLE', () => {
    api.book.mockReturnValueOnce(throwError(() => ({
      error: { error: { code: 'SLOT_UNAVAILABLE' } },
    })));
    component.selectDoctor(doctor);
    component.selectedSlot.set(slot);
    api.listSlots.mockClear();

    component.bookSelectedSlot();

    expect(api.listSlots).toHaveBeenCalled();
    expect(component.error()).toBe('appointments.errors.booking');
  });

  it('annule un rendez-vous après confirmation', () => {
    component.requestCancellation(appointment);
    component.confirmCancellation();

    expect(api.cancel).toHaveBeenCalledWith(appointment.id);
    expect(component.cancelTarget()).toBeNull();
    expect(component.notice()).toBe('appointments.success.cancelled');
  });
});
