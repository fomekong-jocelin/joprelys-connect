import { Component, signal } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import { I18nService } from '../../core/i18n/i18n.service';
import { AppShellComponent } from '../../shared/layout/app-shell.component';
import { IconComponent } from '../../shared/ui/icon.component';
import { PageHeaderComponent } from '../../shared/ui/page-header.component';
import { OrganizationApiService } from '../organizations/organization-api.service';
import { RbacApiService } from '../rbac/rbac-api.service';
import { HospitalOrganizationApiService } from './hospital-organization-api.service';
import { OrganizationalUnit } from './hospital-organization.models';
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

@Component({ selector: 'app-ui-icon', standalone: true, inputs: ['name', 'class'], template: '' })
class IconStubComponent {}

const pole: OrganizationalUnit = {
  id: 'pole-1', parentId: null, code: 'MERE_ENFANT', name: 'Mère-enfant',
  unitType: 'POLE', serviceCatalogCode: null, active: true,
};
const service: OrganizationalUnit = {
  id: 'service-1', parentId: 'pole-1', code: 'SVC_GEN_MED', name: null,
  unitType: 'SERVICE', serviceCatalogCode: 'GENERAL_MEDICINE', active: true,
};

describe('HospitalOrganizationPageComponent', () => {
  let fixture: ComponentFixture<HospitalOrganizationPageComponent>;
  let api: Record<'listServiceCatalog' | 'listSpecialtyCatalog' | 'listUnits' | 'createUnit' | 'updateUnit' | 'setActive', ReturnType<typeof vi.fn>>;
  const locale = signal<'fr' | 'en'>('fr');
  const text = () => (fixture.nativeElement as HTMLElement).textContent ?? '';

  async function setup(units: OrganizationalUnit[]): Promise<void> {
    locale.set('fr');
    api = {
      listServiceCatalog: vi.fn().mockReturnValue(of([
        { code: 'GENERAL_MEDICINE', nameFr: 'Médecine générale', nameEn: 'General medicine' },
      ])),
      listSpecialtyCatalog: vi.fn().mockReturnValue(of([
        { code: 'CARDIOLOGY', nameFr: 'Cardiologie', nameEn: 'Cardiology' },
      ])),
      listUnits: vi.fn().mockReturnValue(of(units)),
      createUnit: vi.fn().mockReturnValue(of({})),
      updateUnit: vi.fn().mockReturnValue(of({})),
      setActive: vi.fn().mockReturnValue(of({})),
    };

    await TestBed.configureTestingModule({
      imports: [HospitalOrganizationPageComponent],
      providers: [
        provideRouter([]),
        { provide: HospitalOrganizationApiService, useValue: api },
        { provide: OrganizationApiService, useValue: { list: vi.fn().mockReturnValue(of([])) } },
        { provide: RbacApiService, useValue: { hasPermission: vi.fn().mockReturnValue(false) } },
        {
          provide: I18nService,
          useValue: {
            locale,
            t: (key: string) => ({
              'hospitalOrg.actions.add.SERVICE': 'Ajouter un service',
              'hospitalOrg.actions.add.CARE_UNIT': 'Ajouter une unité de soins',
              'hospitalOrg.actions.addInto.CARE_UNIT': 'Ajouter une unité de soins dans {unit}',
              'hospitalOrg.actions.editUnit': 'Modifier {unit}',
              'hospitalOrg.catalogs.summary': '{services} types · {specialties} spécialités',
              'hospitalOrg.feedback.error': 'Erreur localisée',
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
  }

  describe('with an existing structure', () => {
    beforeEach(() => setup([service, pole]));

    it('loads the controlled catalogs and the tenant units', () => {
      expect(api.listServiceCatalog).toHaveBeenCalledOnce();
      expect(api.listSpecialtyCatalog).toHaveBeenCalledOnce();
      expect(api.listUnits).toHaveBeenCalledWith(undefined, false);
    });

    it('nests a service under its pole instead of listing units flat', () => {
      const rootItems = fixture.nativeElement.querySelectorAll('section[aria-labelledby="hospital-org-tree-title"] > ul > li');
      expect(rootItems).toHaveLength(1);
      expect(rootItems[0].textContent).toContain('Mère-enfant');
      expect(rootItems[0].querySelector('ul li')?.textContent).toContain('Médecine générale');
    });

    it('shows a catalog-backed service in the active locale', () => {
      expect(text()).toContain('Médecine générale');
      locale.set('en');
      expect(fixture.componentInstance.unitLabel(service)).toBe('General medicine');
    });

    it('names the row action after what it creates and the unit it targets', () => {
      const addCareUnit = fixture.nativeElement.querySelector('button[aria-label="Ajouter une unité de soins dans Médecine générale"]');
      expect(addCareUnit).not.toBeNull();

      addCareUnit.click();

      expect(fixture.componentInstance.editor()).toEqual({ mode: 'create', unitType: 'CARE_UNIT', parentId: 'service-1' });
    });

    it('keeps the reference catalogs folded away', () => {
      const details = fixture.nativeElement.querySelector('details');
      expect(details).not.toBeNull();
      expect(details.open).toBe(false);
      expect(details.querySelector('summary').textContent).toContain('1 types · 1 spécialités');
    });

    it('keeps the editor open and shows the server error inside it', () => {
      api.createUnit.mockReturnValueOnce(throwError(() => new HttpErrorResponse({ status: 409 })));
      const component = fixture.componentInstance;
      component.openCreate('SERVICE');

      component.save({ id: null, payload: {
        code: 'SVC_GEN_MED', unitType: 'SERVICE', parentId: null, name: null, serviceCatalogCode: 'GENERAL_MEDICINE',
      } });
      fixture.detectChanges();

      expect(component.editor()).not.toBeNull();
      expect(fixture.nativeElement.querySelector('[role="dialog"] [role="alert"]')?.textContent).toContain('Erreur localisée');
      expect(fixture.nativeElement.querySelector('main > [role="alert"]')).toBeNull();
    });

    it('closes the editor and reloads after deactivating from it', () => {
      const component = fixture.componentInstance;
      component.openEdit(service);

      component.setActive(service, false);

      expect(api.setActive).toHaveBeenCalledWith('service-1', false, undefined);
      expect(component.editor()).toBeNull();
      expect(api.listUnits).toHaveBeenCalledTimes(2);
    });

    it('shows the localized fallback instead of leaking a backend-language detail', () => {
      api.setActive.mockReturnValueOnce(throwError(() => new HttpErrorResponse({
        status: 409,
        error: { detail: 'Désactivez d abord les unités enfants actives.' },
      })));

      fixture.componentInstance.setActive(pole, false);

      expect(fixture.componentInstance.errorMessage()).toBe('Erreur localisée');
    });

    it('keeps mobile content within the viewport', () => {
      const main = fixture.nativeElement.querySelector('main');
      const primaryAction = fixture.nativeElement.querySelector('button.ui-button-primary');

      expect(main.classList).toContain('overflow-x-hidden');
      expect(primaryAction.classList).toContain('w-full');
      expect(primaryAction.classList).toContain('sm:w-auto');
    });
  });

  describe('without any unit', () => {
    beforeEach(() => setup([]));

    it('offers a single primary action: creating the first service', () => {
      const primaryActions = fixture.nativeElement.querySelectorAll('button.ui-button-primary');

      expect(primaryActions).toHaveLength(1);
      expect(primaryActions[0].textContent).toContain('hospitalOrg.actions.createFirstService');
    });
  });
});
