import { Component, WritableSignal, signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, Router, convertToParamMap, provideRouter } from '@angular/router';
import { AuthTokenStorageService } from '../../auth/auth-token-storage.service';
import { I18nService } from '../../core/i18n/i18n.service';
import { AppShellComponent } from '../layout/app-shell.component';
import { NotFoundPageComponent } from './not-found-page.component';
import { ServerUnavailablePageComponent } from './server-unavailable-page.component';

@Component({ selector: 'app-shell', template: '<ng-content />' })
class ShellStubComponent {}

type TestSession = { role: string; name: string } | null;

function configure(session: WritableSignal<TestSession>, returnUrl: string | null = null): void {
  TestBed.configureTestingModule({
    providers: [
      provideRouter([]),
      { provide: AuthTokenStorageService, useValue: { session } },
      { provide: I18nService, useValue: { t: (key: string) => key } },
      { provide: ActivatedRoute, useValue: { snapshot: { queryParamMap: convertToParamMap(returnUrl ? { returnUrl } : {}) } } },
    ],
  });
  for (const page of [NotFoundPageComponent, ServerUnavailablePageComponent]) {
    TestBed.overrideComponent(page, { remove: { imports: [AppShellComponent] }, add: { imports: [ShellStubComponent] } });
  }
}

describe('NotFoundPageComponent', () => {
  it.each([
    [{ role: 'MEDECIN', name: 'Dr Démo' }, '/dashboard', 'notFound.backToDashboard'],
    [{ role: 'PATIENT', name: 'Patient Démo' }, '/patient/dashboard', 'notFound.backToDashboard'],
    [null, '/', 'notFound.backToHome'],
  ])('renvoie la session %o vers %s', (session, href, label) => {
    configure(signal<TestSession>(session));
    const fixture = TestBed.createComponent(NotFoundPageComponent);
    fixture.detectChanges();

    const link = (fixture.nativeElement as HTMLElement).querySelector('[data-testid="not-found-home"]') as HTMLAnchorElement;
    expect(link.getAttribute('href')).toBe(href);
    expect(link.textContent?.trim()).toBe(label);
    expect((fixture.nativeElement as HTMLElement).querySelector('h1')?.textContent?.trim()).toBe('notFound.title');
  });
});

describe('ServerUnavailablePageComponent', () => {
  it('relance la page interne demandée', async () => {
    configure(signal<TestSession>({ role: 'MEDECIN', name: 'Dr Démo' }), '/clinic/lab');
    const fixture = TestBed.createComponent(ServerUnavailablePageComponent);
    const navigate = vi.spyOn(TestBed.inject(Router), 'navigateByUrl').mockResolvedValue(true);
    fixture.detectChanges();

    ((fixture.nativeElement as HTMLElement).querySelector('[data-testid="server-unavailable-retry"]') as HTMLButtonElement).click();
    await fixture.whenStable();

    expect(navigate).toHaveBeenCalledWith('/clinic/lab');
    expect(fixture.componentInstance.retrying()).toBe(false);
  });

  it('ignore une URL de retour externe et revient au tableau de bord du rôle', async () => {
    configure(signal<TestSession>({ role: 'PATIENT', name: 'Patient Démo' }), '//evil.example/phish');
    const fixture = TestBed.createComponent(ServerUnavailablePageComponent);
    const navigate = vi.spyOn(TestBed.inject(Router), 'navigateByUrl').mockResolvedValue(true);

    await fixture.componentInstance.retry();

    expect(navigate).toHaveBeenCalledWith('/patient/dashboard');
  });
});
