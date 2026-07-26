package com.joprelys.backend.ai.facts.infrastructure.persistence;

import com.joprelys.backend.ai.facts.domain.ClinicalFactTypes.Authority;
import com.joprelys.backend.ai.facts.domain.ClinicalFactTypes.FactStatus;
import com.joprelys.backend.ai.facts.domain.ClinicalFactTypes.FactType;
import com.joprelys.backend.ai.facts.domain.ClinicalFactTypes.Laterality;
import com.joprelys.backend.ai.facts.domain.ClinicalFactTypes.Polarity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.hibernate.annotations.TenantId;

@Entity
@Table(name = "ai_clinical_facts")
public class ClinicalFactEntity {

    @Id
    private UUID id;

    @TenantId
    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "visit_id", nullable = false)
    private UUID visitId;

    @Column(name = "sequence_no", nullable = false)
    private long sequenceNo;

    @Column(name = "source_event_id", nullable = false, length = 200)
    private String sourceEventId;

    @Enumerated(EnumType.STRING)
    @Column(name = "fact_type", nullable = false, length = 32)
    private FactType factType;

    @Enumerated(EnumType.STRING)
    @Column(name = "authority", nullable = false, length = 32)
    private Authority authority;

    @Column(name = "concept_code", nullable = false, length = 64)
    private String conceptCode;

    @Column(name = "concept_text", nullable = false, length = 256)
    private String conceptText;

    @Enumerated(EnumType.STRING)
    @Column(name = "polarity", nullable = false, length = 16)
    private Polarity polarity;

    @Column(name = "value_primary", length = 128)
    private String valuePrimary;

    @Column(name = "value_secondary", length = 128)
    private String valueSecondary;

    @Column(name = "unit_code", length = 32)
    private String unitCode;

    @Column(name = "temporality_text", length = 256)
    private String temporalityText;

    @Enumerated(EnumType.STRING)
    @Column(name = "laterality", nullable = false, length = 16)
    private Laterality laterality;

    @Column(name = "frequency_text", length = 128)
    private String frequencyText;

    @Column(name = "route_text", length = 64)
    private String routeText;

    @Enumerated(EnumType.STRING)
    @Column(name = "fact_status", nullable = false, length = 16)
    private FactStatus factStatus;

    @Column(name = "supersedes_fact_id")
    private UUID supersedesFactId;

    @Column(name = "created_by_user_id", nullable = false)
    private UUID createdByUserId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @OneToMany(mappedBy = "fact", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("primarySupport DESC, quoteStartChar ASC")
    private List<ClinicalFactEvidenceEntity> evidence = new ArrayList<>();

    protected ClinicalFactEntity() {
    }

    public ClinicalFactEntity(
            UUID organizationId,
            UUID visitId,
            long sequenceNo,
            String sourceEventId,
            FactType factType,
            Authority authority,
            String conceptCode,
            String conceptText,
            Polarity polarity,
            String valuePrimary,
            String valueSecondary,
            String unitCode,
            String temporalityText,
            Laterality laterality,
            String frequencyText,
            String routeText,
            FactStatus factStatus,
            UUID supersedesFactId,
            UUID createdByUserId) {
        this.id = UUID.randomUUID();
        this.organizationId = organizationId;
        this.visitId = visitId;
        this.sequenceNo = sequenceNo;
        this.sourceEventId = sourceEventId;
        this.factType = factType;
        this.authority = authority;
        this.conceptCode = conceptCode;
        this.conceptText = conceptText;
        this.polarity = polarity;
        this.valuePrimary = valuePrimary;
        this.valueSecondary = valueSecondary;
        this.unitCode = unitCode;
        this.temporalityText = temporalityText;
        this.laterality = laterality;
        this.frequencyText = frequencyText;
        this.routeText = routeText;
        this.factStatus = factStatus;
        this.supersedesFactId = supersedesFactId;
        this.createdByUserId = createdByUserId;
    }

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = Instant.now();
    }

    public void addEvidence(ClinicalFactEvidenceEntity item) {
        item.attachTo(this);
        evidence.add(item);
    }

    public UUID getId() { return id; }
    public UUID getOrganizationId() { return organizationId; }
    public UUID getVisitId() { return visitId; }
    public long getSequenceNo() { return sequenceNo; }
    public String getSourceEventId() { return sourceEventId; }
    public FactType getFactType() { return factType; }
    public Authority getAuthority() { return authority; }
    public String getConceptCode() { return conceptCode; }
    public String getConceptText() { return conceptText; }
    public Polarity getPolarity() { return polarity; }
    public String getValuePrimary() { return valuePrimary; }
    public String getValueSecondary() { return valueSecondary; }
    public String getUnitCode() { return unitCode; }
    public String getTemporalityText() { return temporalityText; }
    public Laterality getLaterality() { return laterality; }
    public String getFrequencyText() { return frequencyText; }
    public String getRouteText() { return routeText; }
    public FactStatus getFactStatus() { return factStatus; }
    public UUID getSupersedesFactId() { return supersedesFactId; }
    public UUID getCreatedByUserId() { return createdByUserId; }
    public Instant getCreatedAt() { return createdAt; }
    public List<ClinicalFactEvidenceEntity> getEvidence() { return Collections.unmodifiableList(evidence); }
}
