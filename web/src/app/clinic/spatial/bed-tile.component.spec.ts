import { ComponentFixture, TestBed } from '@angular/core/testing';
import { I18nService } from '../../core/i18n/i18n.service';
import { bedFixture } from '../../../testing/bed-fixtures';
import { BedActionId } from './bed-display-state';
import { BedTileComponent } from './bed-tile.component';

describe('BedTileComponent', () => {
  let fixture: ComponentFixture<BedTileComponent>;
  let emitted: BedActionId[];
  const host = () => fixture.nativeElement as HTMLElement;
  const buttons = () => Array.from(host().querySelectorAll<HTMLButtonElement>('button'));

  function render(bed = bedFixture(), permissions = { capacity: true, cleaning: true, maintenance: true }): void {
    fixture.componentRef.setInput('bed', bed);
    fixture.componentRef.setInput('permissions', permissions);
    fixture.detectChanges();
  }

  beforeEach(async () => {
    emitted = [];
    await TestBed.configureTestingModule({
      imports: [BedTileComponent],
      providers: [{ provide: I18nService, useValue: { t: (key: string) => key } }],
    }).compileComponents();
    fixture = TestBed.createComponent(BedTileComponent);
    fixture.componentInstance.act.subscribe((action) => emitted.push(action));
    document.body.appendChild(host());
  });

  afterEach(() => host().remove());

  it('shows a single readable state and names the bed for screen readers', () => {
    render(bedFixture({ bedNumber: 'Lit 10-A', readinessStatus: 'CLEANING', available: false }));

    expect(host().textContent).toContain('spatial.bedState.CLEANING');
    expect(host().querySelector('article')?.getAttribute('aria-label')).toBe('spatial.tracking.bedLabel');
  });

  it('groups the choices of a free bed in a menu', () => {
    render();

    expect(buttons()).toHaveLength(1);
    buttons()[0].click();
    fixture.detectChanges();
    const choices = Array.from(host().querySelectorAll<HTMLButtonElement>('li button'));
    expect(choices.map((choice) => choice.textContent?.trim())).toEqual([
      'spatial.tracking.action.START_CLEANING',
      'spatial.tracking.action.START_MAINTENANCE',
      'spatial.tracking.action.CLOSE',
    ]);

    choices[1].click();
    expect(emitted).toEqual(['START_MAINTENANCE']);
  });

  it('offers a direct button when only one action applies', () => {
    render(bedFixture({ capacityStatus: 'CLOSED', available: false }));

    expect(buttons().map((button) => button.textContent?.trim())).toEqual(['spatial.tracking.action.REOPEN']);
    buttons()[0].click();
    expect(emitted).toEqual(['REOPEN']);
  });

  it('shows no action on an occupied bed or without permission', () => {
    render(bedFixture({ usageStatus: 'OCCUPIED', available: false }));
    expect(buttons()).toHaveLength(0);

    render(bedFixture(), { capacity: false, cleaning: false, maintenance: false });
    expect(buttons()).toHaveLength(0);
  });

  it('disables the actions while another bed action is running', () => {
    fixture.componentRef.setInput('busy', true);
    render(bedFixture({ readinessStatus: 'CLEANING', available: false }));

    expect(buttons().every((button) => button.disabled)).toBe(true);
  });
});
