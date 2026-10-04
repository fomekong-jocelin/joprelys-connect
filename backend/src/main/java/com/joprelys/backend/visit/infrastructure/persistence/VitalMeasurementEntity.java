package com.joprelys.backend.visit.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.TenantId;

/**
 * Mesure de constantes horodatée et signée ; une visite peut en compter plusieurs (réévaluations).
 */
@Entity
@Table(name = "visit_vital_measurements")
public class VitalMeasurementEntity {

	@Id
	private UUID id;

	@TenantId
	@Column(name = "organization_id")
	private UUID organizationId;

	@Column(name = "visit_id", nullable = false)
	private UUID visitId;

	@Column(name = "recorded_by")
	private UUID recordedBy;

	@Column(name = "recorded_by_name")
	private String recordedByName;

	@Column(name = "recorded_at", nullable = false)
	private Instant recordedAt;

	private BigDecimal temperature;
	private BigDecimal weight;
	private Integer height;
	private Integer pulse;
	private Integer systolic;
	private Integer diastolic;
	private Integer spo2;
	private BigDecimal glycemia;

	@Column(name = "respiratory_rate")
	private Integer respiratoryRate;

	@Column(name = "pain_scale")
	private Integer painScale;

	private BigDecimal bmi;

	protected VitalMeasurementEntity() {
	}

	public VitalMeasurementEntity(UUID visitId, UUID recordedBy, String recordedByName, Instant recordedAt, VitalsEntity values) {
		this.id = UUID.randomUUID();
		this.visitId = visitId;
		this.recordedBy = recordedBy;
		this.recordedByName = recordedByName;
		this.recordedAt = recordedAt;
		this.temperature = values.getTemperature();
		this.weight = values.getWeight();
		this.height = values.getHeight();
		this.pulse = values.getPulse();
		this.systolic = values.getSystolic();
		this.diastolic = values.getDiastolic();
		this.spo2 = values.getSpo2();
		this.glycemia = values.getGlycemia();
		this.respiratoryRate = values.getRespiratoryRate();
		this.painScale = values.getPainScale();
		this.bmi = values.getBmi();
	}

	public UUID getId() { return id; }
	public UUID getVisitId() { return visitId; }
	public UUID getRecordedBy() { return recordedBy; }
	public String getRecordedByName() { return recordedByName; }
	public Instant getRecordedAt() { return recordedAt; }
	public BigDecimal getTemperature() { return temperature; }
	public BigDecimal getWeight() { return weight; }
	public Integer getHeight() { return height; }
	public Integer getPulse() { return pulse; }
	public Integer getSystolic() { return systolic; }
	public Integer getDiastolic() { return diastolic; }
	public Integer getSpo2() { return spo2; }
	public BigDecimal getGlycemia() { return glycemia; }
	public Integer getRespiratoryRate() { return respiratoryRate; }
	public Integer getPainScale() { return painScale; }
	public BigDecimal getBmi() { return bmi; }
}
