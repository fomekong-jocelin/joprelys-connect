import { TestBed } from '@angular/core/testing';
import { I18nService } from '../../core/i18n/i18n.service';
import { rbacI18nStub } from '../../../testing/rbac-fixtures';
import { RbacAuditTableComponent } from './rbac-audit-table.component';
import { RbacAuditEntry } from './rbac.models';

describe('RbacAuditTableComponent', () => {
  const entry = (overrides: Partial<RbacAuditEntry>): RbacAuditEntry => ({
    id: 'audit-1',
    actorUserId: 'user-admin',
    action: 'ROLE_CREATED',
    targetType: 'ROLE',
    targetId: 'role-custom',
    details: 'Superviseur caisse',
    createdAt: '2026-11-04T13:30:00Z',
    ...overrides,
  });

  function render(entries: RbacAuditEntry[]) {
    TestBed.configureTestingModule({
      imports: [RbacAuditTableComponent],
      providers: [{ provide: I18nService, useValue: rbacI18nStub() }],
    });
    const fixture = TestBed.createComponent(RbacAuditTableComponent);
    fixture.componentRef.setInput('entries', entries);
    fixture.detectChanges();
    return fixture;
  }

  it('translates known actions and keeps unknown ones readable', () => {
    const fixture = render([entry({}), entry({ id: 'audit-2', action: 'FUTURE_ACTION', details: undefined })]);
    const rows = [...(fixture.nativeElement as HTMLElement).querySelectorAll('tbody tr')];

    expect(rows[0].textContent).toContain('Rôle créé');
    expect(rows[0].textContent).toContain('Superviseur caisse');
    expect(rows[0].textContent).not.toContain('ROLE_CREATED');
    expect(rows[1].textContent).toContain('FUTURE_ACTION');
    expect(rows[1].textContent).toContain('—');
  });

  it('formats the date as dd/MM/yyyy HH:mm', () => {
    const fixture = render([entry({})]);
    const cell = (fixture.nativeElement as HTMLElement).querySelector('tbody td')!;

    expect(cell.textContent?.trim()).toMatch(/^04\/11\/2026 \d{2}:30$/);
  });

  it('shows an empty state and asks the page to refresh', () => {
    const fixture = render([]);
    let refreshed = 0;
    fixture.componentInstance.refresh.subscribe(() => refreshed++);
    const host = fixture.nativeElement as HTMLElement;

    expect(host.textContent).toContain('rbac.audit.empty');
    host.querySelector<HTMLButtonElement>('button')!.click();
    expect(refreshed).toBe(1);
  });
});
