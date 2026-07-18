import { ComponentFixture, TestBed } from '@angular/core/testing';
import { AppointmentSlotPickerComponent } from './appointment-slot-picker.component';
import { AppointmentSlot } from '../../../patient/portal/appointments/patient-appointments.models';

describe('AppointmentSlotPickerComponent', () => {
  let fixture: ComponentFixture<AppointmentSlotPickerComponent>;
  let component: AppointmentSlotPickerComponent;

  const slots: AppointmentSlot[] = [
    { doctorId: 'doctor-1', startAt: '2026-08-03T07:00:00Z', endAt: '2026-08-03T07:30:00Z' },
    { doctorId: 'doctor-1', startAt: '2026-08-03T07:30:00Z', endAt: '2026-08-03T08:00:00Z' },
    { doctorId: 'doctor-1', startAt: '2026-08-04T08:00:00Z', endAt: '2026-08-04T08:30:00Z' },
  ];

  beforeEach(async () => {
    await TestBed.configureTestingModule({ imports: [AppointmentSlotPickerComponent] }).compileComponents();
    fixture = TestBed.createComponent(AppointmentSlotPickerComponent);
    component = fixture.componentInstance;
    fixture.componentRef.setInput('slots', slots);
    fixture.componentRef.setInput('labels', { empty: 'Aucun', select: 'Sélectionner' });
    fixture.componentRef.setInput('locale', 'fr');
    fixture.detectChanges();
  });

  it('regroupe les créneaux par journée locale', () => {
    expect(component.groups().length).toBe(2);
    expect(component.groups()[0].slots.length).toBe(2);
    expect(component.groups()[1].slots.length).toBe(1);
  });

  it('émet le créneau sélectionné au clic', () => {
    const emitted: AppointmentSlot[] = [];
    component.slotSelected.subscribe((slot) => emitted.push(slot));

    const button = (fixture.nativeElement as HTMLElement).querySelector('button') as HTMLButtonElement;
    button.click();

    expect(emitted[0]).toEqual(slots[0]);
  });

  it('expose l’état sélectionné avec aria-pressed', () => {
    fixture.componentRef.setInput('selectedStartAt', slots[0].startAt);
    fixture.detectChanges();
    const button = (fixture.nativeElement as HTMLElement).querySelector('button') as HTMLButtonElement;
    expect(button.getAttribute('aria-pressed')).toBe('true');
  });
});
