package com.joprelys.backend.clinic.infrastructure.persistence;

import com.joprelys.backend.auth.security.TenantContext;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.springframework.stereotype.Component;
import java.util.UUID;

@Component
public class TenantIdentifierResolver implements CurrentTenantIdentifierResolver<UUID> {

	private static final UUID DEFAULT_TENANT = new java.util.UUID(0L, 0L);

	@Override
	public UUID resolveCurrentTenantIdentifier() {
		UUID tenantId = TenantContext.getTenantId();
		return tenantId != null ? tenantId : DEFAULT_TENANT;
	}

	@Override
	public boolean validateExistingCurrentSessions() {
		return true;
	}
}
