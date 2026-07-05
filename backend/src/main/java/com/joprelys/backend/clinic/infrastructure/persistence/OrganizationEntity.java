package com.joprelys.backend.clinic.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "organizations")
public class OrganizationEntity {

	@Id
	private UUID id;

	@Column(nullable = false, length = 150)
	private String name;

	@Column(nullable = false, unique = true, length = 150)
	private String email;

	@Column(length = 50)
	private String phone;

	@Column(length = 255)
	private String address;

	@Column(nullable = false, length = 100)
	private String city;

	@Column(name = "logo_path", length = 255)
	private String logoPath;

	@Column(nullable = false, length = 20)
	private String status;

	@Column(nullable = false, length = 50)
	private String type;

	@Column(nullable = false, length = 100)
	private String country;

	@Column(name = "responsible_name", nullable = false, length = 150)
	private String responsibleName;

	@Column(name = "api_enabled", nullable = false)
	private boolean apiEnabled;

	@Column(nullable = false)
	private Instant createdAt;

	@Column(nullable = false)
	private Instant updatedAt;

	protected OrganizationEntity() {
	}

	public OrganizationEntity(String name, String email, String phone, String address, String city) {
		this.id = UUID.randomUUID();
		this.name = name;
		this.email = email;
		this.phone = phone;
		this.address = address;
		this.city = city;
		this.status = "ACTIVE";
		this.type = "CLINIC";
		this.country = "Cameroun";
		this.responsibleName = "Responsable";
		this.apiEnabled = true;
	}

	public OrganizationEntity(String name, String email, String phone, String address, String city, String country, String type, String responsibleName, boolean apiEnabled) {
		this.id = UUID.randomUUID();
		this.name = name;
		this.email = email;
		this.phone = phone;
		this.address = address;
		this.city = city;
		this.country = country != null ? country : "Cameroun";
		this.type = type != null ? type : "CLINIC";
		this.responsibleName = responsibleName != null ? responsibleName : "Responsable";
		this.apiEnabled = apiEnabled;
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

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getPhone() {
		return phone;
	}

	public void setPhone(String phone) {
		this.phone = phone;
	}

	public String getAddress() {
		return address;
	}

	public void setAddress(String address) {
		this.address = address;
	}

	public String getCity() {
		return city;
	}

	public void setCity(String city) {
		this.city = city;
	}

	public String getLogoPath() {
		return logoPath;
	}

	public void setLogoPath(String logoPath) {
		this.logoPath = logoPath;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getCountry() {
		return country;
	}

	public void setCountry(String country) {
		this.country = country;
	}

	public String getResponsibleName() {
		return responsibleName;
	}

	public void setResponsibleName(String responsibleName) {
		this.responsibleName = responsibleName;
	}

	public boolean isApiEnabled() {
		return apiEnabled;
	}

	public void setApiEnabled(boolean apiEnabled) {
		this.apiEnabled = apiEnabled;
	}
}
