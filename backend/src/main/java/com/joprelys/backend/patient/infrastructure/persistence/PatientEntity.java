package com.joprelys.backend.patient.infrastructure.persistence;

import com.joprelys.backend.patient.domain.IdentityConfidenceLevel;
import com.joprelys.backend.patient.domain.PatientIdentityStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.hibernate.annotations.TenantId;

@Entity
@Table(name = "patients")
public class PatientEntity {

    @Id
    private UUID id;

    @TenantId
    @Column(name = "organization_id")
    private UUID organizationId;

    @Column(name = "global_patient_number", nullable = false, unique = true, length = 50)
    private String globalPatientNumber;

    @Column(name = "local_patient_number", nullable = false, unique = true, length = 50)
    private String localPatientNumber;

    @Column(name = "full_name")
    private String fullName;

    @Column(name = "gender", length = 20)
    private String gender;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Column(name = "phone", length = 50)
    private String phone;

    @Column(name = "city", length = 100)
    private String city;

    @Column(name = "district", length = 100)
    private String district;

    @Column(name = "address")
    private String address;

    @Column(name = "emergency_contact_name", length = 150)
    private String emergencyContactName;

    @Column(name = "emergency_contact_phone", length = 50)
    private String emergencyContactPhone;

    @Column(name = "allergies")
    private String allergies;

    @Column(name = "medical_history")
    private String medicalHistory;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Enumerated(EnumType.STRING)
    @Column(name = "identity_status", nullable = false, length = 32)
    private PatientIdentityStatus identityStatus;

    @Column(name = "temporary_patient_number", unique = true, length = 40)
    private String temporaryPatientNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "identity_confidence_level", nullable = false, length = 24)
    private IdentityConfidenceLevel identityConfidenceLevel;

    @Column(name = "apparent_gender", length = 32)
    private String apparentGender;

    @Column(name = "estimated_age_range", length = 64)
    private String estimatedAgeRange;

    @Column(name = "physical_description", columnDefinition = "TEXT")
    private String physicalDescription;

    @Column(name = "found_at")
    private Instant foundAt;

    @Column(name = "found_location", length = 255)
    private String foundLocation;

    @Column(name = "blood_group", length = 10)
    private String bloodGroup;

    @Column(name = "email", length = 255)
    private String email;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected PatientEntity() {
    }

    public PatientEntity(
            String globalPatientNumber,
            String localPatientNumber,
            String fullName,
            String gender,
            LocalDate birthDate,
            String phone,
            String city,
            String district,
            String address,
            String emergencyContactName,
            String emergencyContactPhone,
            String allergies,
            String medicalHistory) {
        this.id = UUID.randomUUID();
        this.globalPatientNumber = globalPatientNumber;
        this.localPatientNumber = localPatientNumber;
        this.fullName = fullName;
        this.gender = gender;
        this.birthDate = birthDate;
        this.phone = phone;
        this.city = city;
        this.district = district;
        this.address = address;
        this.emergencyContactName = emergencyContactName;
        this.emergencyContactPhone = emergencyContactPhone;
        this.allergies = allergies;
        this.medicalHistory = medicalHistory;
        this.status = "ACTIVE";
        this.identityStatus = PatientIdentityStatus.VERIFIED;
        this.identityConfidenceLevel = IdentityConfidenceLevel.VERIFIED;
    }

    public static PatientEntity provisionalEmergency(
            String globalPatientNumber,
            String localPatientNumber,
            String temporaryPatientNumber,
            String apparentGender,
            String estimatedAgeRange,
            String physicalDescription,
            Instant foundAt,
            String foundLocation,
            IdentityConfidenceLevel confidenceLevel) {
        PatientEntity patient = new PatientEntity();
        patient.id = UUID.randomUUID();
        patient.globalPatientNumber = globalPatientNumber;
        patient.localPatientNumber = localPatientNumber;
        patient.temporaryPatientNumber = temporaryPatientNumber;
        patient.apparentGender = normalize(apparentGender);
        patient.estimatedAgeRange = normalize(estimatedAgeRange);
        patient.physicalDescription = normalize(physicalDescription);
        patient.foundAt = foundAt;
        patient.foundLocation = normalize(foundLocation);
        patient.status = "ACTIVE";
        patient.identityStatus = PatientIdentityStatus.PROVISIONAL_URGENCY;
        patient.identityConfidenceLevel = confidenceLevel == null ? IdentityConfidenceLevel.NONE : confidenceLevel;
        return patient;
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
        if (identityStatus == null) {
            identityStatus = PatientIdentityStatus.VERIFIED;
        }
        if (identityConfidenceLevel == null) {
            identityConfidenceLevel = identityStatus == PatientIdentityStatus.VERIFIED
                    ? IdentityConfidenceLevel.VERIFIED
                    : IdentityConfidenceLevel.NONE;
        }
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }

    public void transitionIdentityStatus(PatientIdentityStatus target) {
        if (target == null) {
            throw new IllegalArgumentException("Le statut d'identité cible est obligatoire");
        }
        if (!identityStatus.canTransitionTo(target)) {
            throw new IllegalStateException("Transition d'identité interdite : " + identityStatus + " -> " + target);
        }
        identityStatus = target;
        if (target == PatientIdentityStatus.VERIFIED) {
            identityConfidenceLevel = IdentityConfidenceLevel.VERIFIED;
        }
    }

    public void restoreIdentityStatusAfterReconciliationCorrection(PatientIdentityStatus target) {
        if (identityStatus != PatientIdentityStatus.MERGED) {
            throw new IllegalStateException("PATIENT_RECONCILIATION_SOURCE_NOT_MERGED");
        }
        if (target == null || target == PatientIdentityStatus.MERGED) {
            throw new IllegalArgumentException("PATIENT_RECONCILIATION_RESTORE_STATUS_INVALID");
        }
        identityStatus = target;
        if (target == PatientIdentityStatus.VERIFIED) {
            identityConfidenceLevel = IdentityConfidenceLevel.VERIFIED;
        }
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

    public String getGlobalPatientNumber() {
        return globalPatientNumber;
    }

    public String getLocalPatientNumber() {
        return localPatientNumber;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = normalize(fullName);
    }

    public String getDisplayName() {
        return fullName != null && !fullName.isBlank() ? fullName : temporaryPatientNumber;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public LocalDate getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(LocalDate birthDate) {
        this.birthDate = birthDate;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getDistrict() {
        return district;
    }

    public void setDistrict(String district) {
        this.district = district;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getEmergencyContactName() {
        return emergencyContactName;
    }

    public void setEmergencyContactName(String emergencyContactName) {
        this.emergencyContactName = emergencyContactName;
    }

    public String getEmergencyContactPhone() {
        return emergencyContactPhone;
    }

    public void setEmergencyContactPhone(String emergencyContactPhone) {
        this.emergencyContactPhone = emergencyContactPhone;
    }

    public String getAllergies() {
        return allergies;
    }

    public void setAllergies(String allergies) {
        this.allergies = allergies;
    }

    public String getMedicalHistory() {
        return medicalHistory;
    }

    public void setMedicalHistory(String medicalHistory) {
        this.medicalHistory = medicalHistory;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public PatientIdentityStatus getIdentityStatus() {
        return identityStatus;
    }

    public String getTemporaryPatientNumber() {
        return temporaryPatientNumber;
    }

    public IdentityConfidenceLevel getIdentityConfidenceLevel() {
        return identityConfidenceLevel;
    }

    public void setIdentityConfidenceLevel(IdentityConfidenceLevel identityConfidenceLevel) {
        this.identityConfidenceLevel = identityConfidenceLevel;
    }

    public String getApparentGender() {
        return apparentGender;
    }

    public String getEstimatedAgeRange() {
        return estimatedAgeRange;
    }

    public String getPhysicalDescription() {
        return physicalDescription;
    }

    public Instant getFoundAt() {
        return foundAt;
    }

    public String getFoundLocation() {
        return foundLocation;
    }

    public String getBloodGroup() {
        return bloodGroup;
    }

    public void setBloodGroup(String bloodGroup) {
        this.bloodGroup = bloodGroup;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
