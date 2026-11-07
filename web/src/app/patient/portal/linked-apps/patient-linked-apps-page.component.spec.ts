import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { Observable, of, throwError } from 'rxjs';
import { provideI18nTesting } from '../../../../testing/i18n-testing';
import { DialogService } from '../../../shared/ui/dialog.service';
import { ToastService } from '../../../shared/ui/toast.service';
import { PatientLinkedAppsApiService } from './linked-apps-api.service';
import { LinkedApp } from './linked-apps.models';
import { PatientLinkedAppsPageComponent } from './patient-linked-apps-page.component';

const ALLOPHARMA: LinkedApp = {
  id: 'link-1',
  application: 'allopharma',
  scopes: ['prescriptions', 'lab-results'],
  linkedAt: '2026-11-07T19:56:00Z',
  validUntil: '2027-11-07T19:56:00Z',
};

function setup(options: { list?: Observable<LinkedApp[]>; confirm?: boolean; revokeFails?: boolean } = {}) {
  const api = {
    list: vi.fn().mockReturnValue(options.list ?? of([ALLOPHARMA])),
    revoke: vi.fn().mockReturnValue(options.revokeFails ? throwError(() => new Error('503')) : of(undefined)),
  };
  const dialog = { confirm: vi.fn().mockResolvedValue(options.confirm ?? true) };
  const toast = { success: vi.fn(), error: vi.fn() };
  TestBed.configureTestingModule({
    imports: [PatientLinkedAppsPageComponent],
    providers: [
      provideHttpClient(),
      provideHttpClientTesting(),
      provideI18nTesting(),
      provideRouter([]),
      { provide: PatientLinkedAppsApiService, useValue: api },
      { provide: DialogService, useValue: dialog },
      { provide: ToastService, useValue: toast },
    ],
  });
  const fixture = TestBed.createComponent(PatientLinkedAppsPageComponent);
  fixture.detectChanges();
  const element = fixture.nativeElement as HTMLElement;
  return { fixture, element, api, dialog, toast };
}

async function clickRevoke(fixture: ReturnType<typeof setup>['fixture'], id = 'link-1') {
  const element = fixture.nativeElement as HTMLElement;
  element.querySelector<HTMLButtonElement>(`[data-testid="linked-app-revoke-${id}"]`)!.click();
  await fixture.whenStable();
  fixture.detectChanges();
}

describe('PatientLinkedAppsPageComponent', () => {
  it('lists the linked apps with their logo, shared data and dates', () => {
    const { element } = setup();

    expect(element.textContent).toContain('AllôPharma');
    expect(element.textContent).toContain('Ordonnances');
    expect(element.textContent).toContain("Résultats d'analyses");
    expect(element.textContent).toContain('07/11/2026');
    expect(element.textContent).toContain('07/11/2027');
    expect(element.querySelector('app-linked-app-card img')?.getAttribute('src')).toBe('/assets/partners/allopharma.png');
  });

  it('names an unknown application generically', () => {
    const { element } = setup({ list: of([{ ...ALLOPHARMA, application: 'future-app' }]) });

    expect(element.textContent).toContain('Application partenaire');
    expect(element.querySelector('app-linked-app-card img')).toBeNull();
  });

  it('shows an empty state when nothing is linked', () => {
    const { element } = setup({ list: of([]) });

    expect(element.textContent).toContain("Aucune application n'accède à votre dossier.");
  });

  it('shows a load error with a working retry', () => {
    const { fixture, element, api } = setup({ list: throwError(() => new Error('offline')) });
    expect(element.querySelector('[data-testid="linked-apps-load-error"]')).not.toBeNull();

    api.list.mockReturnValue(of([ALLOPHARMA]));
    element.querySelector<HTMLButtonElement>('[data-testid="linked-apps-load-error"] button')!.click();
    fixture.detectChanges();

    expect(api.list).toHaveBeenCalledTimes(2);
    expect(element.textContent).toContain('AllôPharma');
  });

  it('revokes after explicit confirmation and removes the app', async () => {
    const { fixture, element, api, dialog, toast } = setup();

    await clickRevoke(fixture);

    expect(dialog.confirm).toHaveBeenCalledWith(expect.objectContaining({ tone: 'danger' }));
    expect(dialog.confirm.mock.calls[0][0].title).toContain('AllôPharma');
    expect(api.revoke).toHaveBeenCalledWith('link-1');
    expect(element.textContent).not.toContain('Ordonnances');
    expect(toast.success).toHaveBeenCalledWith(expect.stringContaining('AllôPharma'));
  });

  it('does nothing when the confirmation is cancelled', async () => {
    const { fixture, element, api } = setup({ confirm: false });

    await clickRevoke(fixture);

    expect(api.revoke).not.toHaveBeenCalled();
    expect(element.textContent).toContain('AllôPharma');
  });

  it('keeps the app listed and says so when revocation fails', async () => {
    const { fixture, element, toast } = setup({ revokeFails: true });

    await clickRevoke(fixture);

    expect(element.textContent).toContain('AllôPharma');
    expect(toast.error).toHaveBeenCalled();
    const button = element.querySelector<HTMLButtonElement>('[data-testid="linked-app-revoke-link-1"]')!;
    expect(button.disabled).toBe(false);
  });

  it('gives the revoke button an accessible name that includes the app', () => {
    const { element } = setup();

    const button = element.querySelector<HTMLButtonElement>('[data-testid="linked-app-revoke-link-1"]')!;
    expect(button.getAttribute('aria-label')).toContain('AllôPharma');
  });
});
