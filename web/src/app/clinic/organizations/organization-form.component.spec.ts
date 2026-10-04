import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { I18nService } from '../../core/i18n/i18n.service';
import { OrganizationFormComponent, OrganizationFormLabels } from './organization-form.component';

describe('OrganizationFormComponent field feedback', () => {
  async function setup() {
    await TestBed.configureTestingModule({ imports: [OrganizationFormComponent], providers: [
      provideHttpClient(), provideHttpClientTesting(),
      { provide: I18nService, useValue: { t: (key: string) => key } },
    ] }).compileComponents();
    const fixture = TestBed.createComponent(OrganizationFormComponent);
    fixture.componentRef.setInput('labels', Object.fromEntries([
      'title', 'name', 'namePlaceholder', 'email', 'emailPlaceholder', 'phone', 'phonePlaceholder',
      'city', 'cityPlaceholder', 'address', 'addressPlaceholder', 'country', 'countryPlaceholder',
      'type', 'responsibleName', 'responsibleNamePlaceholder', 'apiEnabled', 'cancel', 'save', 'saving',
    ].map(key => [key, key])) as unknown as OrganizationFormLabels);
    fixture.detectChanges();
    const submitted = vi.fn(); fixture.componentInstance.submitted.subscribe(submitted);
    return { fixture, component: fixture.componentInstance, submitted };
  }
  it('marks each missing field and binds its error to the input for assistive technology', async () => {
    const { fixture, component, submitted } = await setup();
    component.name.set('   '); component.submit(); fixture.detectChanges();
    expect(submitted).not.toHaveBeenCalled();
    const inputs = fixture.nativeElement.querySelectorAll('input[aria-invalid="true"]');
    expect(inputs).toHaveLength(4);
    for (const input of inputs) expect(fixture.nativeElement.querySelector('#' + input.getAttribute('aria-describedby'))).toBeTruthy();
  });
  it('rejects malformed email then allows a valid complete form', async () => {
    const { fixture, component, submitted } = await setup();
    component.name.set('Clinique'); component.email.set('invalid'); component.city.set('Douala'); component.responsibleName.set('Directeur');
    component.submit(); expect(submitted).not.toHaveBeenCalled();
    expect(component.fieldError('email')).toBe('organizations.emailInvalid');
    component.email.set('contact@clinic.example'); component.submit();
    expect(submitted).toHaveBeenCalledOnce();
    fixture.detectChanges(); expect(fixture.nativeElement.querySelectorAll('input[aria-invalid="true"]')).toHaveLength(0);
  });
});
