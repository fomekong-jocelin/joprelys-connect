package com.joprelys.backend.emergency.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "resuscitation_logs")
public class ResuscitationLogEntity {

	@Id
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "emergency_id", nullable = false)
	private EmergencyEntity emergency;

	@Column(name = "action_type", nullable = false, length = 50)
	private String actionType;

	@Column(name = "description", nullable = false, length = 255)
	private String description;

	@Column(name = "quantity", precision = 10, scale = 2)
	private BigDecimal quantity;

	@Column(name = "unit", length = 20)
	private String unit;

	@Column(name = "administered_at", nullable = false)
	private Instant administeredAt;

	@Column(name = "administered_by_user_id")
	private UUID administeredByUserId;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	protected ResuscitationLogEntity() {
	}

	public ResuscitationLogEntity(
			EmergencyEntity emergency,
			String actionType,
			String description,
			BigDecimal quantity,
			String unit,
			Instant administeredAt,
			UUID administeredByUserId) {
		this.id = UUID.randomUUID();
		this.emergency = emergency;
		this.actionType = actionType;
		this.description = description;
		this.quantity = quantity;
		this.unit = unit;
		this.administeredAt = administeredAt != null ? administeredAt : Instant.now();
		this.administeredByUserId = administeredByUserId;
	}

	@PrePersist
	void prePersist() {
		Instant now = Instant.now();
		createdAt = now;
		updatedAt = now;
	}

	@PreUpdate
	void preUpdate() {
		updatedAt = Instant.now();
	}

	public UUID getId() {
		return id;
	}

	public EmergencyEntity getEmergency() {
		return emergency;
	}

	public void setEmergency(EmergencyEntity emergency) {
		this.emergency = emergency;
	}

	public String getActionType() {
		return actionType;
	}

	public void setActionType(String actionType) {
		this.actionType = actionType;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public BigDecimal getQuantity() {
		return quantity;
	}

	public void setQuantity(BigDecimal quantity) {
		this.quantity = quantity;
	}

	public String getUnit() {
		return unit;
	}

	public void setUnit(String unit) {
		this.unit = unit;
	}

	public Instant getAdministeredAt() {
		return administeredAt;
	}

	public void setAdministeredAt(Instant administeredAt) {
		this.administeredAt = administeredAt;
	}

	public UUID getAdministeredByUserId() {
		return administeredByUserId;
	}

	public void setAdministeredByUserId(UUID administeredByUserId) {
		this.administeredByUserId = administeredByUserId;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}
}
