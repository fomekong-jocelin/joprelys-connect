package com.joprelys.backend.ai.facts.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "ai_clinical_note_validation_facts")
public class ClinicalNoteValidationFactEntity {

    @Id
    private UUID id;

    @Column(name = "validation_id", nullable = false)
    private UUID validationId;

    @Column(name = "fact_id", nullable = false)
    private UUID factId;

    @Column(name = "fact_sequence", nullable = false)
    private long factSequence;

    @Column(name = "section_code", nullable = false, length = 48)
    private String sectionCode;

    @Column(name = "position_no", nullable = false)
    private int positionNo;

    protected ClinicalNoteValidationFactEntity() {
    }

    public ClinicalNoteValidationFactEntity(
            UUID validationId,
            UUID factId,
            long factSequence,
            String sectionCode,
            int positionNo) {
        this.id = UUID.randomUUID();
        this.validationId = validationId;
        this.factId = factId;
        this.factSequence = factSequence;
        this.sectionCode = sectionCode;
        this.positionNo = positionNo;
    }

    public UUID getId() { return id; }
    public UUID getValidationId() { return validationId; }
    public UUID getFactId() { return factId; }
    public long getFactSequence() { return factSequence; }
    public String getSectionCode() { return sectionCode; }
    public int getPositionNo() { return positionNo; }
}
