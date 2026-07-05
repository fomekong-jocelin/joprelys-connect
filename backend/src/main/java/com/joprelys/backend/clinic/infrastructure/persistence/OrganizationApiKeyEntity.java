package com.joprelys.backend.clinic.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "organization_api_keys")
public class OrganizationApiKeyEntity {

	@Id
	private UUID id;

	@Column(name = "organization_id", nullable = false)
	private UUID organizationId;

	@Column(name = "hashed_key", nullable = false, unique = true, length = 64)
	private String hashedKey;

	@Column(nullable = false, length = 20)
	private String prefix;

	@Column(nullable = false, length = 100)
	private String name;

	@Column(nullable = false, length = 20)
	private String status;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Column(name = "revoked_at")
	private Instant revokedAt;

	protected OrganizationApiKeyEntity() {
	}

	public OrganizationApiKeyEntity(UUID organizationId, String hashedKey, String prefix, String name) {
		this.id = UUID.randomUUID();
		this.organizationId = organizationId;
		this.hashedKey = hashedKey;
		this.prefix = prefix;
		this.name = name;
		this.status = "ACTIVE";
		this.createdAt = Instant.now();
	}

	public UUID getId() {
		return id;
	}

	public UUID getOrganizationId() {
		return organizationId;
	}

	public String getHashedKey() {
		return hashedKey;
	}

	public String getPrefix() {
		return prefix;
	}

	public String getName() {
		return name;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getRevokedAt() {
		return revokedAt;
	}

	public void setRevokedAt(Instant revokedAt) {
		this.revokedAt = revokedAt;
	}
}
