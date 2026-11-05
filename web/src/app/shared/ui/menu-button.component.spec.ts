import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MenuButtonComponent } from './menu-button.component';

describe('MenuButtonComponent', () => {
  let fixture: ComponentFixture<MenuButtonComponent>;
  let selected: string[];
  const host = () => fixture.nativeElement as HTMLElement;
  const trigger = () => host().querySelector('button') as HTMLButtonElement;
  const choices = () => Array.from(host().querySelectorAll<HTMLButtonElement>('li button'));

  beforeEach(async () => {
    selected = [];
    await TestBed.configureTestingModule({ imports: [MenuButtonComponent] }).compileComponents();
    fixture = TestBed.createComponent(MenuButtonComponent);
    fixture.componentRef.setInput('label', 'Ajouter');
    fixture.componentRef.setInput('items', [
      { id: 'a', label: 'Pôle', description: 'Regroupe des services' },
      { id: 'b', label: 'Service' },
    ]);
    fixture.componentInstance.selected.subscribe((id) => selected.push(id));
    document.body.appendChild(host());
    fixture.detectChanges();
  });

  afterEach(() => host().remove());

  it('opens a described list and emits the chosen item', () => {
    trigger().click();
    fixture.detectChanges();

    expect(trigger().getAttribute('aria-expanded')).toBe('true');
    expect(choices()[0].textContent).toContain('Regroupe des services');

    choices()[1].click();
    fixture.detectChanges();

    expect(selected).toEqual(['b']);
    expect(choices()).toHaveLength(0);
  });

  it('closes on Escape and gives focus back to the trigger', () => {
    trigger().click();
    fixture.detectChanges();

    choices()[0].dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape', bubbles: true }));
    fixture.detectChanges();

    expect(choices()).toHaveLength(0);
    expect(document.activeElement).toBe(trigger());
  });

  it('closes when the user clicks elsewhere', () => {
    trigger().click();
    fixture.detectChanges();

    document.body.click();
    fixture.detectChanges();

    expect(choices()).toHaveLength(0);
    expect(selected).toEqual([]);
  });

  it('moves focus through the choices with the arrow keys', () => {
    trigger().click();
    fixture.detectChanges();
    choices()[0].focus();

    choices()[0].dispatchEvent(new KeyboardEvent('keydown', { key: 'ArrowDown', bubbles: true }));

    expect(document.activeElement).toBe(choices()[1]);
  });
});
