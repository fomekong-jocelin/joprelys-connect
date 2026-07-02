package com.joprelys.backend.visit.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "vitals")
public class VitalsEntity {

	@Id
	private UUID id;

	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "visit_id", nullable = false, unique = true)
	private VisitEntity visit;

	@Column(name = "temperature", precision = 3, scale = 1)
	private BigDecimal temperature;

	@Column(name = "weight", precision = 4, scale = 1)
	private BigDecimal weight;

	@Column(name = "height")
	private Integer height;

	@Column(name = "pulse")
	private Integer pulse;

	@Column(name = "systolic")
	private Integer systolic;

	@Column(name = "diastolic")
	private Integer diastolic;

	@Column(name = "spo2")
	private Integer spo2;

	@Column(name = "glycemia", precision = 3, scale = 2)
	private BigDecimal glycemia;

	@Column(name = "respiratory_rate")
	private Integer respiratoryRate;

	@Column(name = "bmi", precision = 4, scale = 2)
	private BigDecimal bmi;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	protected VitalsEntity() {
	}

	public VitalsEntity(
			VisitEntity visit,
			BigDecimal temperature,
			BigDecimal weight,
			Integer height,
			Integer pulse,
			Integer systolic,
			Integer diastolic,
			Integer spo2,
			BigDecimal glycemia,
			Integer respiratoryRate,
			BigDecimal bmi) {
		this.id = UUID.randomUUID();
		this.visit = visit;
		this.temperature = temperature;
		this.weight = weight;
		this.height = height;
		this.pulse = pulse;
		this.systolic = systolic;
		this.diastolic = diastolic;
		this.spo2 = spo2;
		this.glycemia = glycemia;
		this.respiratoryRate = respiratoryRate;
		this.bmi = bmi;
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

	public VisitEntity getVisit() {
		return visit;
	}

	public void setVisit(VisitEntity visit) {
		this.visit = visit;
	}

	public BigDecimal getTemperature() {
		return temperature;
	}

	public void setTemperature(BigDecimal temperature) {
		this.temperature = temperature;
	}

	public BigDecimal getWeight() {
		return weight;
	}

	public void setWeight(BigDecimal weight) {
		this.weight = weight;
	}

	public Integer getHeight() {
		return height;
	}

	public void setHeight(Integer height) {
		this.height = height;
	}

	public Integer getPulse() {
		return pulse;
	}

	public void setPulse(Integer pulse) {
		this.pulse = pulse;
	}

	public Integer getSystolic() {
		return systolic;
	}

	public void setSystolic(Integer systolic) {
		this.systolic = systolic;
	}

	public Integer getDiastolic() {
		return diastolic;
	}

	public void setDiastolic(Integer diastolic) {
		this.diastolic = diastolic;
	}

	public Integer getSpo2() {
		return spo2;
	}

	public void setSpo2(Integer spo2) {
		this.spo2 = spo2;
	}

	public BigDecimal getGlycemia() {
		return glycemia;
	}

	public void setGlycemia(BigDecimal glycemia) {
		this.glycemia = glycemia;
	}

	public Integer getRespiratoryRate() {
		return respiratoryRate;
	}

	public void setRespiratoryRate(Integer respiratoryRate) {
		this.respiratoryRate = respiratoryRate;
	}

	public BigDecimal getBmi() {
		return bmi;
	}

	public void setBmi(BigDecimal bmi) {
		this.bmi = bmi;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}
}
