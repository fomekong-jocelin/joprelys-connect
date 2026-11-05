import { ComponentFixture, TestBed } from '@angular/core/testing';
import { I18nService } from '../../core/i18n/i18n.service';
import { RBAC_PERMISSIONS, RBAC_ROLES, rbacI18nStub } from '../../../testing/rbac-fixtures';
import { RbacRoleEditorComponent, RoleSaveEvent } from './rbac-role-editor.component';

describe('RbacRoleEditorComponent', () => {
  let fixture: ComponentFixture<RbacRoleEditorComponent>;
  let component: RbacRoleEditorComponent;
  let saved: RoleSaveEvent[];
  let notices: string[];

  const host = (): HTMLElement => fixture.nativeElement;
  const button = (label: string) => [...host().querySelectorAll<HTMLButtonElement>('button')]
    .find((item) => item.textContent?.includes(label))!;
  const type = (selector: string, value: string) => {
    const field = host().querySelector<HTMLInputElement>(selector)!;
    field.value = value;
    field.dispatchEvent(new Event('input'));
    fixture.detectChanges();
  };
  const submitForm = () => {
    host().querySelector('form')!.dispatchEvent(new Event('submit', { cancelable: true }));
    fixture.detectChanges();
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [RbacRoleEditorComponent],
      providers: [{ provide: I18nService, useValue: rbacI18nStub() }],
    });
    fixture = TestBed.createComponent(RbacRoleEditorComponent);
    component = fixture.componentInstance;
    fixture.componentRef.setInput('roles', RBAC_ROLES);
    fixture.componentRef.setInput('permissions', RBAC_PERMISSIONS);
    saved = [];
    notices = [];
    component.save.subscribe((event) => saved.push(event));
    component.notice.subscribe((message) => notices.push(message));
    fixture.detectChanges();
  });

  it('presents a system role as a read-only template to duplicate', () => {
    expect(host().querySelector('h2')!.textContent).toContain('Administrateur clinique');
    expect(host().textContent).toContain('rbac.roles.systemHelp');
    expect(host().querySelector('form')).toBeNull();
    expect([...host().querySelectorAll<HTMLInputElement>('details input')].every((box) => box.disabled)).toBe(true);
  });

  it('shows permission names grouped by domain, without technical codes', () => {
    const summaries = [...host().querySelectorAll('details summary')].map((item) => item.textContent?.trim());

    expect(summaries[0]).toContain('Caisse');
    expect(summaries[0]).toContain('0 / 2');
    expect(host().textContent).toContain('Encaisser');
    expect(host().textContent).not.toContain('CASH_PAYMENT_COLLECT');
  });

  it('duplicates a system role into an editable copy with a suggested code', () => {
    button('Caissier').click();
    fixture.detectChanges();
    button('rbac.roles.duplicate').click();
    fixture.detectChanges();

    expect(notices).toEqual(['rbac.roles.duplicateReady']);
    expect(host().querySelector<HTMLInputElement>('#rbac-role-name')!.value).toBe('Caissier (copie)');
    expect(host().querySelector<HTMLInputElement>('#rbac-role-code')!.value).toBe('CAISSIER_COPIE');

    submitForm();
    expect(saved).toEqual([{
      id: null,
      request: {
        code: 'CAISSIER_COPIE',
        name: 'Caissier (copie)',
        description: undefined,
        assignable: true,
        permissionCodes: ['CASH_QUEUE_READ', 'CASH_PAYMENT_COLLECT'],
      },
    }]);
  });

  it('creates a role whose code follows the name until the code is edited', () => {
    button('rbac.roles.new').click();
    fixture.detectChanges();
    type('#rbac-role-name', 'Agent d’accueil');
    expect(host().querySelector<HTMLInputElement>('#rbac-role-code')!.value).toBe('AGENT_D_ACCUEIL');

    type('#rbac-role-code', 'accueil-jour');
    type('#rbac-role-name', 'Agent d’accueil de jour');
    host().querySelectorAll<HTMLInputElement>('details input')[0].click();
    fixture.detectChanges();
    submitForm();

    expect(saved).toEqual([{
      id: null,
      request: {
        code: 'ACCUEIL_JOUR',
        name: 'Agent d’accueil de jour',
        description: undefined,
        assignable: true,
        permissionCodes: ['CASH_QUEUE_READ'],
      },
    }]);
  });

  it('blocks an empty name or an invalid code with inline messages', () => {
    button('rbac.roles.new').click();
    fixture.detectChanges();
    submitForm();

    expect(host().textContent).toContain('rbac.roles.validation.nameRequired');
    expect(host().textContent).toContain('rbac.roles.validation.codeFormat');
    expect(saved).toEqual([]);
  });

  it('updates a custom role and its permissions together', () => {
    button('Superviseur caisse').click();
    fixture.detectChanges();
    type('#rbac-role-description', 'Supervise les encaissements.');
    host().querySelectorAll<HTMLInputElement>('details input')[1].click();
    fixture.detectChanges();
    expect(host().textContent).toContain('rbac.users.unsaved');
    submitForm();

    expect(saved).toEqual([{
      id: 'role-custom',
      request: {
        code: 'CAISSE_SUPERVISEUR',
        name: 'Superviseur caisse',
        description: 'Supervise les encaissements.',
        assignable: true,
        enabled: true,
      },
      permissionCodes: ['CASH_QUEUE_READ', 'CASH_PAYMENT_COLLECT'],
    }]);
  });
});
