import { ComponentFixture, TestBed } from '@angular/core/testing';
import {
  WeeklyAvailabilityGridComponent,
  WeeklyAvailabilityGridLabels,
  WeeklyAvailabilityRuleView,
} from './weekly-availability-grid.component';

describe('WeeklyAvailabilityGridComponent', () => {
  let component: WeeklyAvailabilityGridComponent;
  let fixture: ComponentFixture<WeeklyAvailabilityGridComponent>;

  // Volontairement désordonné pour vérifier le tri par heure de début.
  const rules: WeeklyAvailabilityRuleView[] = [
    { id: 'rule-2', weekday: 1, startTime: '14:00', endTime: '18:00', active: false },
    { id: 'rule-1', weekday: 1, startTime: '08:00', endTime: '12:00', active: true },
    { id: 'rule-3', weekday: 3, startTime: '09:00', endTime: '10:00', active: true },
  ];

  const labels: WeeklyAvailabilityGridLabels = {
    weekdays: ['Lun.', 'Mar.', 'Mer.', 'Jeu.', 'Ven.', 'Sam.', 'Dim.'],
    emptyDay: 'Aucune plage',
    inactive: 'Inactive',
    selectDay: 'Ajouter une plage ce jour',
    editRule: 'Modifier cette plage',
    deactivateRule: 'Désactiver',
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [WeeklyAvailabilityGridComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(WeeklyAvailabilityGridComponent);
    component = fixture.componentInstance;
    fixture.componentRef.setInput('rules', rules);
    fixture.componentRef.setInput('labels', labels);
    fixture.detectChanges();
  });

  it('regroupe les règles par jour et les trie par heure de début', () => {
    const columns = component.columns();
    expect(columns.length).toBe(7);
    expect(columns[0].label).toBe('Lun.');
    expect(columns[0].rules.map((rule) => rule.id)).toEqual(['rule-1', 'rule-2']);
    expect(columns[2].rules.map((rule) => rule.id)).toEqual(['rule-3']);
    expect(columns[6].rules.length).toBe(0);
  });

  it('émet weekdaySelected au clic sur un jour', () => {
    let selected: number | null = null;
    component.weekdaySelected.subscribe((value) => (selected = value));

    const dayButtons = (fixture.nativeElement as HTMLElement)
      .querySelectorAll('section > button') as NodeListOf<HTMLButtonElement>;
    dayButtons[2].click();

    expect(selected).toBe(3);
  });

  it('émet ruleSelected au clic sur une plage', () => {
    const emitted: WeeklyAvailabilityRuleView[] = [];
    component.ruleSelected.subscribe((rule) => emitted.push(rule));

    const chipButtons = (fixture.nativeElement as HTMLElement)
      .querySelectorAll('article > button') as NodeListOf<HTMLButtonElement>;
    chipButtons[0].click();

    expect(emitted[0]?.id).toBe('rule-1');
  });

  it('émet ruleDeactivateRequested uniquement pour une plage active', () => {
    const emitted: WeeklyAvailabilityRuleView[] = [];
    component.ruleDeactivateRequested.subscribe((rule) => emitted.push(rule));

    const deactivateButtons = (fixture.nativeElement as HTMLElement)
      .querySelectorAll('article div button') as NodeListOf<HTMLButtonElement>;
    expect(deactivateButtons.length).toBe(2); // rule-1 et rule-3 actives, rule-2 inactive

    deactivateButtons[0].click();
    expect(emitted[0]?.id).toBe('rule-1');
  });

  it('surligne le jour sélectionné', () => {
    fixture.componentRef.setInput('selectedWeekday', 3);
    fixture.detectChanges();

    const sections = (fixture.nativeElement as HTMLElement)
      .querySelectorAll('section') as NodeListOf<HTMLElement>;
    expect(sections[2].style.borderColor).toBe('var(--brand-primary)');
    expect(sections[0].style.borderColor).toBe('var(--app-border)');
  });
});
