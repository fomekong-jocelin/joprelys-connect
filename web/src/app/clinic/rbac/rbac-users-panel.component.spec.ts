import { ComponentFixture, TestBed } from '@angular/core/testing';
import { I18nService } from '../../core/i18n/i18n.service';
import { RBAC_PERMISSIONS, RBAC_ROLES, RBAC_USERS, rbacI18nStub } from '../../../testing/rbac-fixtures';
import { RbacUsersPanelComponent, UserRolesSaveEvent } from './rbac-users-panel.component';

describe('RbacUsersPanelComponent', () => {
  let fixture: ComponentFixture<RbacUsersPanelComponent>;
  let component: RbacUsersPanelComponent;
  let saved: UserRolesSaveEvent[];

  const host = (): HTMLElement => fixture.nativeElement;
  const userButton = (name: string) => [...host().querySelectorAll<HTMLButtonElement>('ul button')]
    .find((button) => button.textContent?.includes(name))!;
  const roleBox = (name: string) => [...host().querySelectorAll('fieldset label')]
    .find((label) => label.textContent?.includes(name))!.querySelector('input') as HTMLInputElement;
  const saveButton = () => [...host().querySelectorAll<HTMLButtonElement>('button')]
    .find((button) => button.textContent?.includes('rbac.users.save'));

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [RbacUsersPanelComponent],
      providers: [{ provide: I18nService, useValue: rbacI18nStub() }],
    });
    fixture = TestBed.createComponent(RbacUsersPanelComponent);
    component = fixture.componentInstance;
    fixture.componentRef.setInput('users', RBAC_USERS);
    fixture.componentRef.setInput('roles', RBAC_ROLES);
    fixture.componentRef.setInput('permissions', RBAC_PERMISSIONS);
    saved = [];
    component.save.subscribe((event) => saved.push(event));
    fixture.detectChanges();
  });

  it('shows each person with readable role names, not codes', () => {
    expect(userButton('Hélène Caisse').textContent).toContain('Caissier');
    expect(host().textContent).not.toContain('CAISSIER');
  });

  it('filters the list by name or email', () => {
    const search = host().querySelector<HTMLInputElement>('#rbac-user-search')!;
    search.value = 'helene';
    search.dispatchEvent(new Event('input'));
    fixture.detectChanges();

    expect(host().querySelectorAll('ul button')).toHaveLength(1);

    search.value = 'nobody';
    search.dispatchEvent(new Event('input'));
    fixture.detectChanges();
    expect(host().textContent).toContain('rbac.users.noMatch');
  });

  it('keeps the signed-in administrator roles read-only', async () => {
    await fixture.whenStable();

    expect(host().textContent).toContain('rbac.users.selfReadOnly');
    expect(roleBox('Caissier').disabled).toBe(true);
    expect(saveButton()).toBeUndefined();

    component.toggleRole('role-cashier');
    component.submit();
    expect(saved).toEqual([]);
  });

  it('saves the checked roles of another person only once something changed', () => {
    userButton('Hélène Caisse').click();
    fixture.detectChanges();
    expect(saveButton()!.disabled).toBe(true);

    roleBox('Administrateur clinique').click();
    fixture.detectChanges();
    expect(host().textContent).toContain('rbac.users.unsaved');
    saveButton()!.click();

    expect(saved).toEqual([{ userId: 'user-cashier', roleIds: ['role-cashier', 'role-admin'] }]);
  });

  it('refuses to remove the last role', () => {
    userButton('Hélène Caisse').click();
    fixture.detectChanges();
    roleBox('Caissier').click();
    fixture.detectChanges();

    expect(host().textContent).toContain('rbac.users.atLeastOneRole');
    expect(saveButton()!.disabled).toBe(true);
  });

  it('lists effective permissions by name, grouped by domain', () => {
    userButton('Hélène Caisse').click();
    fixture.detectChanges();
    const details = host().querySelector('details')!;

    expect(details.querySelector('summary')!.textContent).toContain('Autorisations effectives : 2');
    expect(details.querySelector('h3')!.textContent).toBe('Caisse');
    expect(details.textContent).toContain('Encaisser');
    expect(details.textContent).not.toContain('CASH_PAYMENT_COLLECT');
  });
});
