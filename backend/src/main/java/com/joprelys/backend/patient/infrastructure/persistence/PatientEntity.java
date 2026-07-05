package com.joprelys.backend.patient.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import org.hibernate.annotations.TenantId;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

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

	@Column(name = "full_name", nullable = false)
	private String fullName;

	@Column(name = "gender", nullable = false, length = 20)
	private String gender;

	@Column(name = "birth_date", nullable = false)
	private LocalDate birthDate;

	@Column(name = "phone", length = 50)
	private String phone;

	@Column(name = "city", nullable = false, length = 100)
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
		this.fullName = fullName;
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
