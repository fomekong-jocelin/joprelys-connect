export interface AuditLog {
  id: string;
  actorUserId: string;
  actorName?: string;
  actorOrganizationId: string;
  patientId: string;
  patientName?: string;
  resourceType: string;
  resourceId: string;
  action: string;
  reason?: string;
  ipAddress?: string;
  userAgent?: string;
  status: 'SUCCESS' | 'DENIED';
  createdAt: string;
}
