package com.joprelys.backend.appointment.infrastructure.persistence;

import com.joprelys.backend.auth.infrastructure.persistence.UserAccountEntity;
import com.joprelys.backend.patient.infrastructure.persistence.PatientEntity;
import com.joprelys.backend.visit.infrastructure.persistence.VisitEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.TenantId;

@Entity
@Table(name = "appointments")
public class AppointmentEntity {

	@Id
	private UUID id;

	@TenantId
	@Column(name = "organization_id")
	private UUID organizationId;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "doctor_id", nullable = false)
	private UserAccountEntity doctor;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "patient_id", nullable = false)
	private PatientEntity patient;

	@Column(name = "start_at", nullable = false)
	private Instant startAt;

	@Column(name = "end_at", nullable = false)
	private Instant endAt;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 32)
	private AppointmentStatus status;

	@Column(name = "reason", columnDefinition = "TEXT")
	private String reason;

	@Column(name = "cancelled_at")
	private Instant cancelledAt;

	@Column(name = "cancellation_reason")
	private String cancellationReason;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "visit_id")
	private VisitEntity visit;

	@Column(name = "reminder_sent_at")
	private Instant reminderSentAt;

	// Colonne technique anti double réservation : = start_at si statut actif, NULL si annulé.
	@Column(name = "active_start_at")
	private Instant activeStartAt;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	protected AppointmentEntity() {
	}

	public AppointmentEntity(
			UserAccountEntity doctor,
			PatientEntity patient,
			Instant startAt,
			Instant endAt,
			String reason) {
		this.id = UUID.randomUUID();
		this.doctor = doctor;
		this.patient = patient;
		this.startAt = startAt;
		this.endAt = endAt;
		this.reason = reason;
		this.status = AppointmentStatus.CONFIRMED;
	}

	@PrePersist
	void prePersist() {
		Instant now = Instant.now();
		createdAt = now;
		updatedAt = now;
		refreshActiveStartAt();
	}

	@PreUpdate
	void preUpdate() {
		updatedAt = Instant.now();
		refreshActiveStartAt();
	}

	// Seules les annulations libèrent le créneau ; COMPLETED et NO_SHOW conservent la trace.
	private void refreshActiveStartAt() {
		activeStartAt = isActiveStatus(status) ? startAt : null;
	}

	private static boolean isActiveStatus(AppointmentStatus value) {
		return value == AppointmentStatus.CONFIRMED
				|| value == AppointmentStatus.COMPLETED
				|| value == AppointmentStatus.NO_SHOW;
	}

	public void cancel(AppointmentStatus cancelledStatus, String cancellationReason) {
		if (cancelledStatus != AppointmentStatus.CANCELLED_BY_PATIENT
				&& cancelledStatus != AppointmentStatus.CANCELLED_BY_CLINIC) {
			throw new IllegalArgumentException("Statut d'annulation invalide : " + cancelledStatus);
		}
		this.status = cancelledStatus;
		this.cancelledAt = Instant.now();
		this.cancellationReason = cancellationReason;
	}

	public void linkVisit(VisitEntity visit) {
		this.visit = visit;
	}

	public UUID getId() {
		return id;
	}

	public UUID getOrganizationId() {
		return organizationId;
	}

	public UserAccountEntity getDoctor() {
		return doctor;
	}

	public PatientEntity getPatient() {
		return patient;
	}

	public Instant getStartAt() {
		return startAt;
	}

	public void setStartAt(Instant startAt) {
		this.startAt = startAt;
	}

	public Instant getEndAt() {
		return endAt;
	}

	public void setEndAt(Instant endAt) {
		this.endAt = endAt;
	}

	public AppointmentStatus getStatus() {
		return status;
	}

	public void setStatus(AppointmentStatus status) {
		this.status = status;
	}

	public String getReason() {
		return reason;
	}

	public void setReason(String reason) {
		this.reason = reason;
	}

	public Instant getCancelledAt() {
		return cancelledAt;
	}

	public String getCancellationReason() {
		return cancellationReason;
	}

	public VisitEntity getVisit() {
		return visit;
	}

	public Instant getReminderSentAt() {
		return reminderSentAt;
	}

	public void setReminderSentAt(Instant reminderSentAt) {
		this.reminderSentAt = reminderSentAt;
	}

	public Instant getActiveStartAt() {
		return activeStartAt;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}
}
