package com.joprelys.backend.hospitalization.infrastructure.persistence;

import jakarta.persistence.*;
import org.hibernate.annotations.TenantId;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "surgical_implants")
public class SurgicalImplantEntity {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "operating_report_id", nullable = false)
    private OperatingReportEntity operatingReport;

    @Column(name = "hospitalization_id", nullable = false)
    private UUID hospitalizationId;

    @TenantId
    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "implant_name", nullable = false)
    private String implantName;

    @Column(name = "lot_number")
    private String lotNumber;

    @Column(name = "quantity", nullable = false)
    private int quantity = 1;

    @Column(name = "unit_price", nullable = false)
    private double unitPrice = 0.0;

    @Column(name = "manufacturer")
    private String manufacturer;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    private Long version;

    public SurgicalImplantEntity() {
        this.id = UUID.randomUUID();
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public SurgicalImplantEntity(OperatingReportEntity operatingReport, UUID hospitalizationId, String implantName, int quantity, double unitPrice) {
        this();
        this.operatingReport = operatingReport;
        this.hospitalizationId = hospitalizationId;
        this.implantName = implantName;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public OperatingReportEntity getOperatingReport() { return operatingReport; }
    public void setOperatingReport(OperatingReportEntity operatingReport) { this.operatingReport = operatingReport; }

    public UUID getHospitalizationId() { return hospitalizationId; }
    public void setHospitalizationId(UUID hospitalizationId) { this.hospitalizationId = hospitalizationId; }

    public UUID getOrganizationId() { return organizationId; }
    public void setOrganizationId(UUID organizationId) { this.organizationId = organizationId; }

    public String getImplantName() { return implantName; }
    public void setImplantName(String implantName) { this.implantName = implantName; }

    public String getLotNumber() { return lotNumber; }
    public void setLotNumber(String lotNumber) { this.lotNumber = lotNumber; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public double getUnitPrice() { return unitPrice; }
    public void setUnitPrice(double unitPrice) { this.unitPrice = unitPrice; }

    public String getManufacturer() { return manufacturer; }
    public void setManufacturer(String manufacturer) { this.manufacturer = manufacturer; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
}
