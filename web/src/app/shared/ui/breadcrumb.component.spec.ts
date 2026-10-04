import { Component, signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { readFileSync } from 'node:fs';
import { I18nService } from '../../core/i18n/i18n.service';
import { BreadcrumbComponent } from './breadcrumb.component';

@Component({ template: '' })
class RouteTarget {}

describe('BreadcrumbComponent locale changes', () => {
  it('translates profile and RBAC labels on navigation and without renavigation after switching locale', async () => {
    const locale = signal<'fr' | 'en'>('fr');
    const dictionaries = Object.fromEntries(['fr', 'en'].map(language => [language,
      JSON.parse(readFileSync(`src/assets/i18n/${language}.json`, 'utf8')) as Record<string, string>,
    ]));
    TestBed.configureTestingModule({ imports: [BreadcrumbComponent], providers: [
      provideRouter([
        { path: 'profile', component: RouteTarget, data: { breadcrumb: 'breadcrumb.profile' } },
        { path: 'clinic/rbac', component: RouteTarget, data: { breadcrumb: 'breadcrumb.clinic.rbac' } },
      ]),
      { provide: I18nService, useValue: { t: (key: string) => dictionaries[locale()][key] ?? key } },
    ] });
    const router = TestBed.inject(Router);
    await router.navigateByUrl('/profile');
    const fixture = TestBed.createComponent(BreadcrumbComponent);
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain(dictionaries['fr']['breadcrumb.profile']);
    locale.set('en'); fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain(dictionaries['en']['breadcrumb.profile']);
    await router.navigateByUrl('/clinic/rbac'); fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain(dictionaries['en']['breadcrumb.clinic.rbac']);
    locale.set('fr'); fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain(dictionaries['fr']['breadcrumb.clinic.rbac']);
    expect(fixture.nativeElement.querySelector('nav').getAttribute('aria-label')).toBe(dictionaries['fr']['breadcrumb.navigation']);
  });
});
