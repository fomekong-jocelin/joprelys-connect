export const CASH_WORKSPACE_PERMISSIONS = [
  'CASH_SESSION_OPEN',
  'CASH_SESSION_CLOSE',
  'CASH_MOVEMENT_WRITE',
  'CASH_HISTORY_READ',
] as const;

export const BILLING_MANAGEMENT_PERMISSION_GROUPS = [
  ['BILLING_INVOICE_READ', 'PATIENT_READ'],
  ['BILLING_INVOICE_WRITE'],
  ['RECEIVABLE_REMINDER_READ'],
  ['RECEIVABLE_REMINDER_WRITE'],
  ['INSURANCE_BORDEREAU_READ'],
  ['INSURANCE_BORDEREAU_PROGRESS'],
  ['INSURANCE_BORDEREAU_SETTLE'],
  ['ACCOUNTING_DASHBOARD_READ'],
  ['ACCOUNTING_EXPORT'],
] as const;

export const PROFESSIONAL_ACCESS_POLICIES = {
  billingWorkspace: [
    'BILLING_INVOICE_READ',
    'BILLING_INVOICE_WRITE',
    'RECEIVABLE_REMINDER_READ',
    'RECEIVABLE_REMINDER_WRITE',
    'INSURANCE_BORDEREAU_READ',
    'INSURANCE_BORDEREAU_PROGRESS',
    'INSURANCE_BORDEREAU_SETTLE',
    'ACCOUNTING_DASHBOARD_READ',
    'ACCOUNTING_EXPORT',
  ],
  cashWorkspace: CASH_WORKSPACE_PERMISSIONS,
  cashier: ['CASH_QUEUE_READ', 'CASH_PAYMENT_COLLECT'],
  labQueue: ['LAB_QUEUE_READ'],
  spatial: ['HOSPITALIZATION_READ'],
} as const;

export function hasAnyPermission(
  effectivePermissions: ReadonlySet<string>,
  requiredPermissions: readonly string[],
): boolean {
  return requiredPermissions.some((permission) => effectivePermissions.has(permission));
}

export function hasEveryPermission(
  effectivePermissions: ReadonlySet<string>,
  requiredPermissions: readonly string[],
): boolean {
  return requiredPermissions.every((permission) => effectivePermissions.has(permission));
}

export function hasAnyPermissionGroup(
  effectivePermissions: ReadonlySet<string>,
  permissionGroups: readonly (readonly string[])[],
): boolean {
  return permissionGroups.some((group) => hasEveryPermission(effectivePermissions, group));
}

export function canAccessBillingManagement(
  effectivePermissions: ReadonlySet<string>,
): boolean {
  return hasAnyPermissionGroup(effectivePermissions, BILLING_MANAGEMENT_PERMISSION_GROUPS);
}
