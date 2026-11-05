import { signal } from '@angular/core';
import { RbacPermission, RbacRole, RbacUserAccess } from '../app/clinic/rbac/rbac.models';

/** Données fictives de l'écran « Rôles et autorisations » (aucune personne réelle). */
export const RBAC_PERMISSIONS: RbacPermission[] = [
  { code: 'CASH_QUEUE_READ', domain: 'CAISSE', name: 'Consulter la file' },
  { code: 'CASH_PAYMENT_COLLECT', domain: 'CAISSE', name: 'Encaisser' },
  { code: 'RBAC_MANAGE', domain: 'ADMINISTRATION', name: 'Gérer les rôles' },
];

export const RBAC_ROLES: RbacRole[] = [
  {
    id: 'role-admin',
    code: 'ADMIN_CLINIQUE',
    name: 'Administrateur clinique',
    systemRole: true,
    assignable: true,
    enabled: true,
    permissions: ['RBAC_MANAGE'],
  },
  {
    id: 'role-cashier',
    code: 'CAISSIER',
    name: 'Caissier',
    systemRole: true,
    assignable: true,
    enabled: true,
    permissions: ['CASH_QUEUE_READ', 'CASH_PAYMENT_COLLECT'],
  },
  {
    id: 'role-custom',
    code: 'CAISSE_SUPERVISEUR',
    name: 'Superviseur caisse',
    description: 'Contrôle la caisse.',
    systemRole: false,
    assignable: true,
    enabled: true,
    permissions: ['CASH_QUEUE_READ'],
  },
];

export const RBAC_USERS: RbacUserAccess[] = [
  {
    id: 'user-admin',
    email: 'admin@test.local',
    displayName: 'Admin clinique',
    enabled: true,
    currentUser: true,
    roles: ['ADMIN_CLINIQUE'],
    permissions: ['RBAC_MANAGE'],
  },
  {
    id: 'user-cashier',
    email: 'caisse@test.local',
    displayName: 'Hélène Caisse',
    enabled: true,
    currentUser: false,
    roles: ['CAISSIER'],
    permissions: ['CASH_QUEUE_READ', 'CASH_PAYMENT_COLLECT'],
  },
];

const DICTIONARY: Record<string, string> = {
  'rbac.roles.copyName': '{name} (copie)',
  'rbac.users.effectivePermissions': 'Autorisations effectives : {count}',
  'rbac.domain.CAISSE': 'Caisse',
  'rbac.domain.ADMINISTRATION': 'Administration',
  'rbac.audit.actions.ROLE_CREATED': 'Rôle créé',
};

/**
 * Faux I18nService au comportement du vrai (`dictionnaire ?? défaut ?? clé`) : les autres textes
 * s'affichent sous forme de clé, ce qui permet de vérifier lequel est montré.
 */
export function rbacI18nStub(locale: 'fr' | 'en' = 'fr') {
  return {
    t: (key: string, fallback?: string) => DICTIONARY[key] ?? fallback ?? key,
    locale: signal(locale),
  };
}
