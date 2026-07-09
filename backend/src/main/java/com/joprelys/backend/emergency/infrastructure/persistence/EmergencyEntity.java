package com.joprelys.backend.emergency.infrastructure.persistence;

import com.joprelys.backend.patient.infrastructure.persistence.PatientEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import org.hibernate.annotations.TenantId;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "emergencies")
public class EmergencyEntity {

	@Id
	private UUID id;

	@TenantId
	@Column(name = "organization_id")
	private UUID organizationId;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "patient_id", nullable = false)
	private PatientEntity patient;

	@Column(name = "visit_id")
	private UUID visitId;

	@Column(name = "arrival_mode", nullable = false, length = 50)
	private String arrivalMode;

	@Column(name = "triage_level", nullable = false, length = 20)
	private String triageLevel;

	@Column(name = "hemodynamic_status", nullable = false, length = 50)
	private String hemodynamicStatus;

	@Column(name = "chief_complaint", nullable = false, columnDefinition = "TEXT")
	private String chiefComplaint;

	@Column(name = "initial_bp_systolic")
	private Integer initialBpSystolic;

	@Column(name = "initial_bp_diastolic")
	private Integer initialBpDiastolic;

	@Column(name = "initial_hr")
	private Integer initialHr;

	@Column(name = "initial_temp", precision = 4, scale = 2)
	private BigDecimal initialTemp;

	@Column(name = "stabilized_at")
	private Instant stabilizedAt;

	@Column(name = "orientation", length = 50)
	private String orientation;

	@Column(name = "created_by_user_id")
	private UUID createdByUserId;

	@OneToMany(mappedBy = "emergency", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
	private List<ResuscitationLogEntity> resuscitationLogs = new ArrayList<>();

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	protected EmergencyEntity() {
	}

	public EmergencyEntity(
			PatientEntity patient,
			UUID visitId,
			String arrivalMode,
			String triageLevel,
			String hemodynamicStatus,
			String chiefComplaint,
			Integer initialBpSystolic,
			Integer initialBpDiastolic,
			Integer initialHr,
			BigDecimal initialTemp,
			UUID createdByUserId) {
		this.id = UUID.randomUUID();
		this.patient = patient;
		this.visitId = visitId;
		this.arrivalMode = arrivalMode;
		this.triageLevel = triageLevel;
		this.hemodynamicStatus = hemodynamicStatus;
		this.chiefComplaint = chiefComplaint;
		this.initialBpSystolic = initialBpSystolic;
		this.initialBpDiastolic = initialBpDiastolic;
		this.initialHr = initialHr;
		this.initialTemp = initialTemp;
		this.createdByUserId = createdByUserId;
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

	public UUID getOrganizationId() {
		return organizationId;
	}

	public void setOrganizationId(UUID organizationId) {
		this.organizationId = organizationId;
	}

	public PatientEntity getPatient() {
		return patient;
	}

	public void setPatient(PatientEntity patient) {
		this.patient = patient;
	}

	public UUID getVisitId() {
		return visitId;
	}

	public void setVisitId(UUID visitId) {
		this.visitId = visitId;
	}

	public String getArrivalMode() {
		return arrivalMode;
	}

	public void setArrivalMode(String arrivalMode) {
		this.arrivalMode = arrivalMode;
	}

	public String getTriageLevel() {
		return triageLevel;
	}

	public void setTriageLevel(String triageLevel) {
		this.triageLevel = triageLevel;
	}

	public String getHemodynamicStatus() {
		return hemodynamicStatus;
	}

	public void setHemodynamicStatus(String hemodynamicStatus) {
		this.hemodynamicStatus = hemodynamicStatus;
	}

	public String getChiefComplaint() {
		return chiefComplaint;
	}

	public void setChiefComplaint(String chiefComplaint) {
		this.chiefComplaint = chiefComplaint;
	}

	public Integer getInitialBpSystolic() {
		return initialBpSystolic;
	}

	public void setInitialBpSystolic(Integer initialBpSystolic) {
		this.initialBpSystolic = initialBpSystolic;
	}

	public Integer getInitialBpDiastolic() {
		return initialBpDiastolic;
	}

	public void setInitialBpDiastolic(Integer initialBpDiastolic) {
		this.initialBpDiastolic = initialBpDiastolic;
	}

	public Integer getInitialHr() {
		return initialHr;
	}

	public void setInitialHr(Integer initialHr) {
		this.initialHr = initialHr;
	}

	public BigDecimal getInitialTemp() {
		return initialTemp;
	}

	public void setInitialTemp(BigDecimal initialTemp) {
		this.initialTemp = initialTemp;
	}

	public Instant getStabilizedAt() {
		return stabilizedAt;
	}

	public void setStabilizedAt(Instant stabilizedAt) {
		this.stabilizedAt = stabilizedAt;
	}

	public String getOrientation() {
		return orientation;
	}

	public void setOrientation(String orientation) {
		this.orientation = orientation;
	}

	public UUID getCreatedByUserId() {
		return createdByUserId;
	}

	public void setCreatedByUserId(UUID createdByUserId) {
		this.createdByUserId = createdByUserId;
	}

	public List<ResuscitationLogEntity> getResuscitationLogs() {
		return resuscitationLogs;
	}

	public void addResuscitationLog(ResuscitationLogEntity log) {
		resuscitationLogs.add(log);
		log.setEmergency(this);
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}
}
