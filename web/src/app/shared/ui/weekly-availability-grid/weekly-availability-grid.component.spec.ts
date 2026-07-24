import { ComponentFixture, TestBed } from '@angular/core/testing';
import {
  WeeklyAvailabilityExceptionView,
  WeeklyAvailabilityGridComponent,
  WeeklyAvailabilityGridLabels,
  WeeklyAvailabilityRangeSelection,
  WeeklyAvailabilityRuleView,
} from './weekly-availability-grid.component';

describe('WeeklyAvailabilityGridComponent', () => {
  let component: WeeklyAvailabilityGridComponent;
  let fixture: ComponentFixture<WeeklyAvailabilityGridComponent>;

  const weekStart = new Date(2026, 6, 20); // lundi 20/07/2026
  const rules: WeeklyAvailabilityRuleView[] = [
    {
      id: 'rule-1',
      weekday: 1,
      startTime: '08:00',
      endTime: '12:00',
      active: true,
      validFrom: '2026-01-01',
      validTo: null,
    },
    {
      id: 'rule-inactive',
      weekday: 1,
      startTime: '14:00',
      endTime: '18:00',
      active: false,
      validFrom: '2026-01-01',
      validTo: null,
    },
    {
      id: 'rule-3',
      weekday: 3,
      startTime: '09:00',
      endTime: '10:00',
      active: true,
      validFrom: '2026-01-01',
      validTo: '2026-12-31',
    },
  ];

  const exceptions: WeeklyAvailabilityExceptionView[] = [
    {
      id: 'exception-1',
      startAt: new Date(2026, 6, 21, 10, 0).toISOString(),
      endAt: new Date(2026, 6, 21, 12, 0).toISOString(),
      reason: 'Formation',
    },
  ];

  const labels: WeeklyAvailabilityGridLabels = {
    weekdays: ['Lun.', 'Mar.', 'Mer.', 'Jeu.', 'Ven.', 'Sam.', 'Dim.'],
    emptyDay: 'Aucune plage',
    inactive: 'Inactive',
    selectDay: 'Ajouter une plage ce jour',
    editRule: 'Modifier cette plage',
    deactivateRule: 'Désactiver',
    available: 'Disponible',
    unavailable: 'Indisponible',
    clickToAdd: 'Cliquer pour ajouter',
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [WeeklyAvailabilityGridComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(WeeklyAvailabilityGridComponent);
    component = fixture.componentInstance;
    fixture.componentRef.setInput('rules', rules);
    fixture.componentRef.setInput('exceptions', exceptions);
    fixture.componentRef.setInput('weekStart', weekStart);
    fixture.componentRef.setInput('labels', labels);
    fixture.detectChanges();
  });

  it('construit sept colonnes datées du lundi au dimanche', () => {
    const days = component.days();

    expect(days).toHaveLength(7);
    expect(days[0].label).toBe('Lun.');
    expect(days[0].date.getDate()).toBe(20);
    expect(days[6].date.getDate()).toBe(26);
  });

  it('n’affiche dans le calendrier que les règles actives et valides pour la semaine', () => {
    const monday = component.days()[0];
    const wednesday = component.days()[2];

    expect(monday.rules.map((rule) => rule.id)).toEqual(['rule-1']);
    expect(wednesday.rules.map((rule) => rule.id)).toEqual(['rule-3']);
    expect(component.blocksForDay(monday).some((block) => block.kind === 'availability')).toBe(true);
  });

  it('positionne les indisponibilités sur le jour civil concerné', () => {
    const tuesday = component.days()[1];
    const blocks = component.blocksForDay(tuesday);

    expect(tuesday.exceptions.map((exception) => exception.id)).toEqual(['exception-1']);
    expect(blocks.some((block) => block.kind === 'exception' && block.subtitle === 'Formation')).toBe(true);
  });

  it('émet weekdaySelected depuis l’en-tête du jour', () => {
    let selected: number | null = null;
    component.weekdaySelected.subscribe((value) => (selected = value));

    const dayButtons = (fixture.nativeElement as HTMLElement)
      .querySelectorAll('.grid-cols-\[64px_repeat\(7,minmax\(0,1fr\)\)\] > button') as NodeListOf<HTMLButtonElement>;
    dayButtons[2].click();

    expect(selected).toBe(3);
  });

  it('prépare une plage d’une heure arrondie à 30 minutes depuis une zone vide', () => {
    let selection: WeeklyAvailabilityRangeSelection | null = null;
    component.rangeSelected.subscribe((value) => (selection = value));
    const day = component.days()[1];
    const currentTarget = {
      getBoundingClientRect: () => ({ top: 0, height: component.calendarHeight() }),
    } as HTMLElement;
    const event = {
      target: document.createElement('div'),
      currentTarget,
      clientY: component.rowHeight * 2.25,
    } as unknown as MouseEvent;

    component.selectRange(event, day);

    expect(selection?.weekday).toBe(2);
    expect(selection?.startTime).toBe('10:00');
    expect(selection?.endTime).toBe('11:00');
    expect(selection?.validFrom).toBe('2026-07-21');
  });

  it('émet ruleSelected au clic sur une plage visible', () => {
    const emitted: WeeklyAvailabilityRuleView[] = [];
    component.ruleSelected.subscribe((rule) => emitted.push(rule));

    const ruleButton = (fixture.nativeElement as HTMLElement)
      .querySelector('[data-calendar-block] button') as HTMLButtonElement;
    ruleButton.click();

    expect(emitted[0]?.id).toBe('rule-1');
  });

  it('garde une amplitude horaire suffisante même sans données tôt ou tard', () => {
    expect(component.calendarStartHour()).toBeLessThanOrEqual(8);
    expect(component.calendarEndHour()).toBeGreaterThanOrEqual(18);
    expect(component.calendarHeight()).toBeGreaterThan(0);
  });
});
