import { ComponentFixture, TestBed } from '@angular/core/testing';
import { signal } from '@angular/core';
import { I18nService } from '../../../core/i18n/i18n.service';
import { bedFixture } from '../../../../testing/bed-fixtures';
import { FacilityLocationNode, FacilitySpace } from '../spatial-configuration.models';
import { PlaceEditorComponent } from './place-editor.component';
import { PlaceEditorRequest, PlaceSaveEvent } from './place-editor.models';

describe('PlaceEditorComponent', () => {
  let fixture: ComponentFixture<PlaceEditorComponent>;
  let saved: PlaceSaveEvent[];
  let removed: number;
  const host = () => fixture.nativeElement as HTMLElement;
  const component = () => fixture.componentInstance;

  const building: FacilityLocationNode = { id: 'b', parentId: null, code: 'BAT_A', name: 'Bâtiment A', nodeType: 'BUILDING', active: true };
  const floor: FacilityLocationNode = { id: 'f', parentId: 'b', code: 'ETAGE_1', name: 'Étage 1', nodeType: 'FLOOR', active: true };
  const room: FacilitySpace = {
    id: 'r', locationNodeId: 'f', code: 'CHAMBRE_201', name: 'Chambre 201', spaceTypeCode: 'HOSPITAL_ROOM', inpatientProfile: true, active: true,
  };

  function open(request: PlaceEditorRequest): void {
    fixture = TestBed.createComponent(PlaceEditorComponent);
    fixture.componentRef.setInput('request', request);
    fixture.componentRef.setInput('locations', [building, floor]);
    fixture.componentRef.setInput('spaces', [room]);
    fixture.componentRef.setInput('spaceTypes', [
      { code: 'HOSPITAL_ROOM', nameFr: 'Chambre', nameEn: 'Room', inpatientCompatible: true },
      { code: 'WAITING_ROOM', nameFr: 'Attente', nameEn: 'Waiting', inpatientCompatible: false },
    ]);
    fixture.componentRef.setInput('unitOptions', [{ id: 'unit-1', label: 'Cardiologie' }]);
    component().save.subscribe((event) => saved.push(event));
    component().remove.subscribe(() => removed++);
    document.body.appendChild(host());
    fixture.detectChanges();
  }

  beforeEach(async () => {
    saved = [];
    removed = 0;
    await TestBed.configureTestingModule({
      imports: [PlaceEditorComponent],
      providers: [{ provide: I18nService, useValue: { t: (key: string) => key, locale: signal('fr') } }],
    }).compileComponents();
  });

  afterEach(() => host().remove());

  it('suggests a free code from the name and saves a normalized location', () => {
    open({ kind: 'location', parentId: 'b' });
    component().name.set('Étage 1');
    component().nodeType.set('FLOOR');

    expect(component().code()).toBe('ETAGE_1_2');
    component().submit();

    expect(saved).toEqual([{
      kind: 'location',
      id: null,
      payload: { parentId: 'b', code: 'ETAGE_1_2', name: 'Étage 1', nodeType: 'FLOOR' },
    }]);
  });

  it('shows the errors only after a submit attempt and keeps the dialog open', () => {
    open({ kind: 'space' });
    expect(host().querySelector('#place-name')?.getAttribute('aria-invalid')).toBe('false');

    component().submit();
    fixture.detectChanges();

    expect(saved).toHaveLength(0);
    expect(host().querySelector('#place-name')?.getAttribute('aria-invalid')).toBe('true');
    expect(host().textContent).toContain('spatial.editor.validation.nameRequired');
  });

  it('never offers a place inside itself as its parent', () => {
    open({ kind: 'location', location: building });

    expect(component().parentOptions().map((location) => location.id)).toEqual([]);
  });

  it('sends the comfort level only for a room that receives inpatients', () => {
    open({ kind: 'space', locationNodeId: 'f' });
    component().name.set('Chambre 202');
    component().enableInpatientProfile.set(true);
    component().comfortLevel.set('VIP');
    component().submit();

    expect(saved[0]).toMatchObject({ kind: 'space', comfortLevel: 'VIP', payload: { enableInpatientProfile: true, locationNodeId: 'f' } });
  });

  it('locks the room type once the room has beds', async () => {
    open({ kind: 'space', space: room, comfortLevel: 'ISOLATION' });
    await fixture.whenStable();

    expect(component().inpatientLocked()).toBe(true);
    expect((host().querySelector('#space-type') as HTMLSelectElement).disabled).toBe(true);
    expect(component().code()).toBe('CHAMBRE_201');
  });

  it('confirms a bed removal in two steps', () => {
    open({ kind: 'bed', space: room, bed: bedFixture({ bedNumber: '201-A' }) });
    const removeButton = () => Array.from(host().querySelectorAll<HTMLButtonElement>('footer button'))
      .find((button) => button.textContent?.includes('spatial.editor.bed.remove'))!;

    removeButton().click();
    fixture.detectChanges();
    expect(removed).toBe(0);
    expect(host().textContent).toContain('spatial.editor.bed.removeConfirm');

    removeButton().click();
    expect(removed).toBe(1);
  });

  it('refuses to delete an occupied bed', () => {
    open({ kind: 'bed', space: room, bed: bedFixture({ usageStatus: 'OCCUPIED', available: false }) });

    expect(component().removalBlocked()).toBe(true);
    expect(host().textContent).toContain('spatial.editor.bed.removeBlocked');
  });

  it('rejects a link that ends before it starts', () => {
    open({ kind: 'assignment', space: room });
    component().validFrom.set('2026-11-05T10:00');
    component().validTo.set('2026-11-01T10:00');
    component().submit();

    expect(saved).toHaveLength(0);
    expect(component().errors().order).toBe('spatial.editor.validation.periodOrder');

    component().validTo.set('');
    component().submit();
    expect(saved[0]).toMatchObject({ kind: 'assignment', payload: { organizationalUnitId: 'unit-1', spaceId: 'r', validTo: null } });
  });
});
