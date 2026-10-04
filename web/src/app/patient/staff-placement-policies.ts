import { HospitalPractitioner } from './hospitalization-workflow.models';

export function isEligibleHospitalizationPractitioner(
  member: HospitalPractitioner,
  organizationalUnitId: string,
): boolean {
  if (!organizationalUnitId || !member.enabled || !hasRole(member.role, 'MEDECIN')) return false;
  return member.activeOrganizationalUnits?.some((unit) => unit.id === organizationalUnitId) ?? false;
}

function hasRole(roleValue: string, expectedRole: string): boolean {
  return roleValue.split(',').map((role) => role.trim()).includes(expectedRole);
}
