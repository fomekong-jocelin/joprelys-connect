package com.joprelys.backend.visit.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "visit_corrections")
public class VisitCorrectionEntity {

	@Id
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "visit_id", nullable = false)
	private VisitEntity visit;

	@Column(name = "corrected_by_user_id")
	private UUID correctedByUserId;

	@Column(name = "correction_reason", nullable = false, columnDefinition = "TEXT")
	private String correctionReason;

	@Column(name = "previous_values", nullable = false, columnDefinition = "TEXT")
	private String previousValues;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	protected VisitCorrectionEntity() {
	}

	public VisitCorrectionEntity(
			VisitEntity visit,
			UUID correctedByUserId,
			String correctionReason,
			String previousValues) {
		this.id = UUID.randomUUID();
		this.visit = visit;
		this.correctedByUserId = correctedByUserId;
		this.correctionReason = correctionReason;
		this.previousValues = previousValues;
	}

	@PrePersist
	void prePersist() {
		createdAt = Instant.now();
	}

	public UUID getId() {
		return id;
	}

	public VisitEntity getVisit() {
		return visit;
	}

	public UUID getCorrectedByUserId() {
		return correctedByUserId;
	}

	public String getCorrectionReason() {
		return correctionReason;
	}

	public String getPreviousValues() {
		return previousValues;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}
}
