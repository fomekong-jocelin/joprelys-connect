import { signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { I18nService } from '../../core/i18n/i18n.service';
import { OrganizationalUnit } from './hospital-organization.models';
import { OrgUnitEditorComponent, UnitEditorRequest, UnitSaveEvent } from './org-unit-editor.component';

const pole: OrganizationalUnit = {
  id: 'pole-1', parentId: null, code: 'MERE_ENFANT', name: 'Mère-enfant',
  unitType: 'POLE', serviceCatalogCode: null, active: true,
};
const service: OrganizationalUnit = {
  id: 'service-1', parentId: null, code: 'GENERAL_MEDICINE', name: null,
  unitType: 'SERVICE', serviceCatalogCode: 'GENERAL_MEDICINE', active: true,
};

describe('OrgUnitEditorComponent', () => {
  let fixture: ComponentFixture<OrgUnitEditorComponent>;
  let saved: UnitSaveEvent[];

  async function open(request: UnitEditorRequest, units: OrganizationalUnit[] = [pole, service]) {
    saved = [];
    await TestBed.configureTestingModule({
      imports: [OrgUnitEditorComponent],
      providers: [{ provide: I18nService, useValue: { locale: signal('fr'), t: (key: string) => key } }],
    }).compileComponents();
    fixture = TestBed.createComponent(OrgUnitEditorComponent);
    fixture.componentRef.setInput('request', request);
    fixture.componentRef.setInput('units', units);
    fixture.componentRef.setInput('serviceCatalog', [
      { code: 'GENERAL_MEDICINE', nameFr: 'Médecine générale', nameEn: 'General medicine' },
      { code: 'PEDIATRICS', nameFr: 'Pédiatrie', nameEn: 'Pediatrics' },
    ]);
    fixture.componentInstance.save.subscribe((event) => saved.push(event));
    fixture.detectChanges();
    return fixture.componentInstance;
  }

  const query = (selector: string) => (fixture.nativeElement as HTMLElement).querySelector(selector);

  it('renders an accessible dialog that explains every level', async () => {
    await open({ mode: 'create', unitType: 'SERVICE', parentId: null });

    const dialog = query('[role="dialog"]');
    expect(dialog?.getAttribute('aria-modal')).toBe('true');
    expect(dialog?.classList).toContain('overflow-x-hidden');
    expect((fixture.nativeElement as HTMLElement).querySelectorAll('input[type="radio"]')).toHaveLength(4);
    expect(dialog?.textContent).toContain('hospitalOrg.levelHelp.CARE_UNIT');
  });

  it('only offers the levels a parent accepts', async () => {
    const editor = await open({ mode: 'create', unitType: 'SERVICE', parentId: 'pole-1' });

    expect(editor.levelChoices()).toEqual(['DEPARTMENT', 'SERVICE']);
    expect(editor.unitType()).toBe('SERVICE');
  });

  it('submits the catalog identity with a suggested code and no free-text name', async () => {
    const editor = await open({ mode: 'create', unitType: 'SERVICE', parentId: null });
    editor.serviceCatalogCode.set('PEDIATRICS');

    editor.submit();

    expect(saved).toEqual([{ id: null, payload: {
      code: 'PEDIATRICS', unitType: 'SERVICE', parentId: null, name: null, serviceCatalogCode: 'PEDIATRICS',
    } }]);
  });

  it('suggests a free code from the local name and keeps the discipline', async () => {
    const editor = await open({ mode: 'create', unitType: 'SERVICE', parentId: null });
    editor.serviceCatalogCode.set('GENERAL_MEDICINE');
    editor.name.set('  Médecine générale  ');

    editor.submit();

    expect(saved[0].payload).toEqual(expect.objectContaining({
      code: 'MEDECINE_GENERALE', name: 'Médecine générale', serviceCatalogCode: 'GENERAL_MEDICINE',
    }));
  });

  it('keeps a code the user typed and normalizes it like the server', async () => {
    const editor = await open({ mode: 'create', unitType: 'POLE', parentId: null });
    editor.name.set('Chirurgie');
    editor.editCode('bloc-central');

    editor.submit();

    expect(saved[0].payload.code).toBe('BLOC_CENTRAL');
  });

  it('requires a service parent for a care unit and says why', async () => {
    const editor = await open({ mode: 'create', unitType: 'CARE_UNIT', parentId: null });
    editor.name.set('Soins intensifs');

    editor.submit();
    fixture.detectChanges();

    expect(saved).toHaveLength(0);
    expect(query('#org-parent-error')?.textContent).toContain('hospitalOrg.validation.parentRequired');
    expect(query('#org-parent')?.getAttribute('aria-invalid')).toBe('true');
  });

  it('tells the user to create a service first when none exists', async () => {
    const editor = await open({ mode: 'create', unitType: 'CARE_UNIT', parentId: null }, [pole]);

    expect(editor.parentError()).toBe('hospitalOrg.validation.noServiceYet');
  });

  it('drops a parent that no longer accepts the chosen level', async () => {
    const editor = await open({ mode: 'create', unitType: 'CARE_UNIT', parentId: 'service-1' });

    editor.selectLevel('SERVICE');

    expect(editor.parentId()).toBeNull();
  });

  it('shows validation only after a submit attempt', async () => {
    const editor = await open({ mode: 'create', unitType: 'SERVICE', parentId: null });
    expect(query('#service-catalog-error')).toBeNull();

    editor.submit();
    fixture.detectChanges();

    expect(query('#service-catalog-error')?.textContent).toContain('hospitalOrg.validation.serviceTypeRequired');
  });

  it('edits an existing unit without changing its level and offers deactivation', async () => {
    const editor = await open({ mode: 'edit', unit: service });
    const deactivated: OrganizationalUnit[] = [];
    editor.deactivate.subscribe((unit) => deactivated.push(unit));

    expect(editor.levelChoices()).toEqual(['SERVICE']);
    expect(query('input[type="radio"]')).toBeNull();
    (query('footer .ui-button-danger') as HTMLButtonElement).click();
    editor.submit();

    expect(deactivated).toEqual([service]);
    expect(saved[0]).toEqual(expect.objectContaining({ id: 'service-1' }));
    expect(saved[0].payload.code).toBe('GENERAL_MEDICINE');
  });

  it('displays the server error inside the dialog', async () => {
    await open({ mode: 'create', unitType: 'SERVICE', parentId: null });
    fixture.componentRef.setInput('error', 'Ce code est déjà utilisé.');
    fixture.detectChanges();

    expect(query('[role="dialog"] [role="alert"]')?.textContent).toContain('Ce code est déjà utilisé.');
  });
});
