import { Component, signal } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { I18nService } from '../../core/i18n/i18n.service';
import { AppShellComponent } from '../../shared/layout/app-shell.component';
import { IconComponent } from '../../shared/ui/icon.component';
import { PageHeaderComponent } from '../../shared/ui/page-header.component';
import { OrganizationApiService } from '../organizations/organization-api.service';
import { RbacApiService } from '../rbac/rbac-api.service';
import { HospitalOrganizationApiService } from './hospital-organization-api.service';
import { HospitalOrganizationPageComponent } from './hospital-organization-page.component';

@Component({ selector: 'app-shell', standalone: true, template: '<ng-content />' })
class AppShellStubComponent {}

@Component({
  selector: 'app-page-header',
  standalone: true,
  inputs: ['title', 'subtitle', 'backLink', 'backLabel'],
  template: '<ng-content />',
})
class PageHeaderStubComponent {}

@Component({ selector: 'app-ui-icon', standalone: true, inputs: ['name'], template: '' })
class IconStubComponent {}

describe('HospitalOrganizationPageComponent', () => {
  let fixture: ComponentFixture<HospitalOrganizationPageComponent>;
  let api: {
    listServiceCatalog: ReturnType<typeof vi.fn>;
    listSpecialtyCatalog: ReturnType<typeof vi.fn>;
    listUnits: ReturnType<typeof vi.fn>;
    createUnit: ReturnType<typeof vi.fn>;
    updateUnit: ReturnType<typeof vi.fn>;
    setActive: ReturnType<typeof vi.fn>;
  };
  const locale = signal<'fr' | 'en'>('fr');

  beforeEach(async () => {
    locale.set('fr');
    api = {
      listServiceCatalog: vi.fn().mockReturnValue(of([
        {
          code: 'GENERAL_MEDICINE',
          nameFr: 'Médecine générale',
          nameEn: 'General medicine',
        },
      ])),
      listSpecialtyCatalog: vi.fn().mockReturnValue(of([
        { code: 'GENERAL_MEDICINE', nameFr: 'Médecine générale', nameEn: 'General medicine' },
      ])),
      listUnits: vi.fn().mockReturnValue(of([
        {
          id: 'service-1',
          parentId: null,
          code: 'SVC_GEN_MED',
          name: null,
          unitType: 'SERVICE',
          serviceCatalogCode: 'GENERAL_MEDICINE',
          active: true,
        },
      ])),
      createUnit: vi.fn().mockReturnValue(of({})),
      updateUnit: vi.fn().mockReturnValue(of({})),
      setActive: vi.fn().mockReturnValue(of({})),
    };

    await TestBed.configureTestingModule({
      imports: [HospitalOrganizationPageComponent],
      providers: [
        { provide: HospitalOrganizationApiService, useValue: api },
        { provide: OrganizationApiService, useValue: { list: vi.fn().mockReturnValue(of([])) } },
        { provide: RbacApiService, useValue: { hasPermission: vi.fn().mockReturnValue(false) } },
        {
          provide: I18nService,
          useValue: {
            locale,
            t: (key: string) => ({
              'hospitalOrg.title': 'Organisation hospitalière',
              'hospitalOrg.unitType.SERVICE': 'Service',
              'hospitalOrg.actions.addService': 'Ajouter un service',
              'hospitalOrg.feedback.error': 'Erreur localisée',
              'common.edit': 'Modifier',
              'common.loading': 'Chargement',
              'common.cancel': 'Annuler',
              'common.save': 'Enregistrer',
              'common.saving': 'Enregistrement',
            } as Record<string, string>)[key] ?? key,
          },
        },
      ],
    })
      .overrideComponent(HospitalOrganizationPageComponent, {
        remove: { imports: [AppShellComponent, PageHeaderComponent, IconComponent] },
        add: { imports: [AppShellStubComponent, PageHeaderStubComponent, IconStubComponent] },
      })
      .compileComponents();

    fixture = TestBed.createComponent(HospitalOrganizationPageComponent);
    fixture.detectChanges();
  });

  it('loads controlled catalogs and tenant units', () => {
    expect(api.listServiceCatalog).toHaveBeenCalledOnce();
    expect(api.listSpecialtyCatalog).toHaveBeenCalledOnce();
    expect(api.listUnits).toHaveBeenCalledWith(undefined, false);
    expect(fixture.componentInstance.units()).toHaveLength(1);
  });

  it('renders a catalog-backed service using the active locale instead of stored free text', () => {
    expect(fixture.nativeElement.textContent).toContain('Médecine générale');

    locale.set('en');
    fixture.detectChanges();

    expect(fixture.componentInstance.unitLabel(fixture.componentInstance.units()[0])).toBe('General medicine');
  });

  it('does not expose a free-text service name when creating a service', () => {
    fixture.componentInstance.openCreate('SERVICE');
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('#service-catalog')).not.toBeNull();
    expect(fixture.nativeElement.querySelector('#org-unit-name')).toBeNull();
  });

  it('requires a service parent for a care unit', () => {
    fixture.componentInstance.openCreate('CARE_UNIT');
    const form = fixture.componentInstance.editor();

    expect(form).not.toBeNull();
    expect(fixture.componentInstance.canSubmit({ ...form!, code: 'UNIT_A', name: 'Unité A' })).toBe(false);
  });

  it('submits service catalog identity without a free-text name', () => {
    fixture.componentInstance.openCreate('SERVICE');
    const form = fixture.componentInstance.editor()!;
    form.code = 'SVC_GEN_MED';
    form.serviceCatalogCode = 'GENERAL_MEDICINE';

    fixture.componentInstance.submit();

    expect(api.createUnit).toHaveBeenCalledWith({
      code: 'SVC_GEN_MED',
      unitType: 'SERVICE',
      parentId: null,
      name: null,
      serviceCatalogCode: 'GENERAL_MEDICINE',
    }, undefined);
  });

  it('shows the localized fallback instead of leaking a backend-language detail', () => {
    api.setActive.mockReturnValueOnce(throwError(() => new HttpErrorResponse({
      status: 409,
      error: { detail: 'Désactivez d abord les unités enfants actives.' },
    })));

    fixture.componentInstance.setActive(fixture.componentInstance.units()[0], false);

    expect(fixture.componentInstance.errorMessage()).toBe('Erreur localisée');
  });

  it('keeps mobile content within the viewport without tree indentation', () => {
    const main = fixture.nativeElement.querySelector('main');
    const unitCard = fixture.nativeElement.querySelector('article');
    const primaryAction = fixture.nativeElement.querySelector('button.ui-button-primary');

    expect(main.classList).toContain('overflow-x-hidden');
    expect(unitCard.hasAttribute('style')).toBe(false);
    expect(primaryAction.classList).toContain('w-full');
    expect(primaryAction.classList).toContain('sm:w-auto');
  });

  it('renders the editor as an accessible responsive dialog', () => {
    fixture.componentInstance.openCreate('SERVICE');
    fixture.detectChanges();

    const dialog = fixture.nativeElement.querySelector('[role="dialog"]');
    expect(dialog).not.toBeNull();
    expect(dialog.getAttribute('aria-modal')).toBe('true');
    expect(dialog.classList).toContain('overflow-x-hidden');
    expect(dialog.classList).toContain('min-w-0');
  });
});
