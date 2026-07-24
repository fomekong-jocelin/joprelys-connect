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

  it('préremplit le formulaire depuis une sélection horaire du calendrier', () => {
    const selectedDate = new Date(2026, 6, 21);

    component.onCalendarRangeSelected({
      weekday: 2,
      date: selectedDate,
      validFrom: toLocalDateKey(selectedDate),
      startTime: '10:30',
      endTime: '11:30',
    });

    expect(component.showRuleForm()).toBe(true);
    expect(component.formWeekday()).toBe(2);
    expect(component.formStartTime()).toBe('10:30');
    expect(component.formEndTime()).toBe('11:30');
    expect(component.formValidFrom()).toBe('2026-07-21');
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
