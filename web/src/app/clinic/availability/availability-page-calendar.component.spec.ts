import { signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { I18nService } from '../../core/i18n/i18n.service';
import { AvailabilityApiService } from './availability-api.service';
import { AvailabilityPageComponent } from './availability-page.component';
import { toLocalDateKey } from './availability-slots.util';

describe('AvailabilityPageComponent calendar UX', () => {
  let fixture: ComponentFixture<AvailabilityPageComponent>;
  let component: AvailabilityPageComponent;

  beforeEach(async () => {
    const api = {
      listRules: vi.fn().mockReturnValue(of([])),
      createRule: vi.fn(),
      updateRule: vi.fn(),
      deactivateRule: vi.fn(),
      listExceptions: vi.fn().mockReturnValue(of([])),
      createException: vi.fn(),
      deleteException: vi.fn(),
    };

    await TestBed.configureTestingModule({
      imports: [AvailabilityPageComponent],
      providers: [
        provideRouter([]),
        { provide: AvailabilityApiService, useValue: api },
        {
          provide: I18nService,
          useValue: {
            t: (key: string) => key,
            locale: signal('fr'),
          },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(AvailabilityPageComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  afterEach(() => fixture.destroy());

  it('garde les trois panneaux secondaires repliés au chargement', () => {
    expect(component.rulesExpanded()).toBe(false);
    expect(component.exceptionsExpanded()).toBe(false);
    expect(component.slotsExpanded()).toBe(false);

    const disclosures = (fixture.nativeElement as HTMLElement)
      .querySelectorAll('section.ui-card > button[aria-expanded]');
    expect(disclosures.length).toBe(3);
    expect(Array.from(disclosures).every((element) => element.getAttribute('aria-expanded') === 'false')).toBe(true);
  });

  it('affiche un calendrier principal même lorsqu’aucune donnée n’est configurée', () => {
    expect(fixture.nativeElement.querySelector('app-weekly-availability-grid')).not.toBeNull();
    expect(component.rules()).toEqual([]);
    expect(component.exceptions()).toEqual([]);
  });

  it('navigue exactement d’une semaine avec les contrôles précédent/suivant', () => {
    const initial = component.weekStart().getTime();

    component.nextWeek();
    expect(component.weekStart().getTime() - initial).toBe(7 * 24 * 60 * 60 * 1000);

    component.previousWeek();
    expect(component.weekStart().getTime()).toBe(initial);
  });

  it('préremplit la disponibilité depuis une sélection horaire et l’ouvre dans une modal', () => {
    const selectedDate = new Date(2026, 6, 21);

    component.onCalendarRangeSelected({
      weekday: 2,
      date: selectedDate,
      validFrom: toLocalDateKey(selectedDate),
      startTime: '10:30',
      endTime: '11:30',
    });
    fixture.detectChanges();

    expect(component.showRuleForm()).toBe(true);
    expect(component.formWeekday()).toBe(2);
    expect(component.formStartTime()).toBe('10:30');
    expect(component.formEndTime()).toBe('11:30');
    expect(component.formValidFrom()).toBe('2026-07-21');

    const dialog = (fixture.nativeElement as HTMLElement).querySelector('[role="dialog"][aria-labelledby="availability-dialog-title"]');
    expect(dialog).not.toBeNull();
    expect(dialog?.closest('.fixed.inset-0')).not.toBeNull();
  });

  it('ouvre la création d’indisponibilité dans une modal dédiée', () => {
    component.toggleExceptionForm();
    fixture.detectChanges();

    const dialog = (fixture.nativeElement as HTMLElement).querySelector('[role="dialog"][aria-labelledby="availability-dialog-title"]');
    expect(component.showExceptionForm()).toBe(true);
    expect(dialog).not.toBeNull();
    expect(dialog?.closest('.fixed.inset-0')).not.toBeNull();
  });

  it('ferme la modal de disponibilité via l’action Annuler', () => {
    component.toggleRuleForm();
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('#availability-dialog-title')).not.toBeNull();

    component.cancelRuleForm();
    fixture.detectChanges();

    expect(component.showRuleForm()).toBe(false);
    expect(fixture.nativeElement.querySelector('#availability-dialog-title')).toBeNull();
  });

  it('déplie les détails seulement sur action explicite', () => {
    component.toggleRulesPanel();
    component.toggleExceptionsPanel();
    component.toggleSlotsPanel();

    expect(component.rulesExpanded()).toBe(true);
    expect(component.exceptionsExpanded()).toBe(true);
    expect(component.slotsExpanded()).toBe(true);
  });
});
