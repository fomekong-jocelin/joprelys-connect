package com.joprelys.backend.auth.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "users")
public class UserAccountEntity {

	@Id
	private UUID id;

	@Column(nullable = false, unique = true, length = 320)
	private String email;

	@Column(nullable = false, length = 160)
	private String displayName;

	@Column(nullable = false, length = 64)
	private String role;

	@Column(nullable = false, length = 120)
	private String passwordHash;

	@Column(nullable = false)
	private boolean enabled;

	@Column(name = "organization_id")
	private UUID organizationId;

	@Column(name = "last_login_at")
	private Instant lastLoginAt;

	@Column(nullable = false)
	private Instant createdAt;

	@Column(nullable = false)
	private Instant updatedAt;

	@Column(name = "photo_path", length = 255)
	private String photoPath;

	@Column(name = "signature_path", length = 255)
	private String signaturePath;

	@Column(name = "stamp_path", length = 255)
	private String stampPath;

	@Column(length = 50)
	private String phone;

	@Column(length = 150)
	private String specialty;

	@Column(name = "registration_number", length = 100)
	private String registrationNumber;

	@Column(length = 150)
	private String department;

	@Column(columnDefinition = "TEXT")
	private String bio;

	protected UserAccountEntity() {
	}

	public UserAccountEntity(String email, String displayName, String role, String passwordHash) {
		this.id = UUID.randomUUID();
		this.email = email;
		this.displayName = displayName;
		this.role = role;
		this.passwordHash = passwordHash;
		this.enabled = true;
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

	public String getEmail() {
		return email;
	}

	public String getDisplayName() {
		return displayName;
	}

	public String getRole() {
		return role;
	}

	public String getPasswordHash() {
		return passwordHash;
	}

	public void setPasswordHash(String passwordHash) {
		this.passwordHash = passwordHash;
	}

	public boolean isEnabled() {
		return enabled;
	}

	public UUID getOrganizationId() {
		return organizationId;
	}

	public void setOrganizationId(UUID organizationId) {
		this.organizationId = organizationId;
	}

	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}

	public void setRole(String role) {
		this.role = role;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public Instant getLastLoginAt() {
		return lastLoginAt;
	}

	public void setLastLoginAt(Instant lastLoginAt) {
		this.lastLoginAt = lastLoginAt;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}

	public String getPhotoPath() {
		return photoPath;
	}

	public void setPhotoPath(String photoPath) {
		this.photoPath = photoPath;
	}

	public String getSignaturePath() {
		return signaturePath;
	}

	public void setSignaturePath(String signaturePath) {
		this.signaturePath = signaturePath;
	}

	public String getStampPath() {
		return stampPath;
	}

	public void setStampPath(String stampPath) {
		this.stampPath = stampPath;
	}

	public String getPhone() {
		return phone;
	}

	public void setPhone(String phone) {
		this.phone = phone;
	}

	public String getSpecialty() {
		return specialty;
	}

	public void setSpecialty(String specialty) {
		this.specialty = specialty;
	}

	public String getRegistrationNumber() {
		return registrationNumber;
	}

	public void setRegistrationNumber(String registrationNumber) {
		this.registrationNumber = registrationNumber;
	}

	public String getDepartment() {
		return department;
	}

	public void setDepartment(String department) {
		this.department = department;
	}

	public String getBio() {
		return bio;
	}

	public void setBio(String bio) {
		this.bio = bio;
	}

	public boolean hasRole(String roleName) {
		if (this.role == null) return false;
		return java.util.Arrays.stream(this.role.split(","))
				.map(String::trim)
				.anyMatch(roleName::equalsIgnoreCase);
	}
}
