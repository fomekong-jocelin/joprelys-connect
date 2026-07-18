export const CASH_WORKSPACE_PERMISSIONS = [
  'CASH_SESSION_OPEN',
  'CASH_SESSION_CLOSE',
  'CASH_MOVEMENT_WRITE',
  'CASH_HISTORY_READ',
] as const;

export const PROFESSIONAL_ACCESS_POLICIES = {
  billingWorkspace: [
    'BILLING_INVOICE_READ',
    'INSURANCE_BORDEREAU_READ',
    'ACCOUNTING_DASHBOARD_READ',
    ...CASH_WORKSPACE_PERMISSIONS,
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
