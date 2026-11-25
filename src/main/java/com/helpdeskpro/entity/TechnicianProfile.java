package com.helpdeskpro.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Size;

@Entity
@Table(name = "technician_profile")
public class TechnicianProfile {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Size(max = 30)
	@Column(name = "phone", length = 30)
	private String phone;

	@Size(max = 100)
	@Column(name = "specialization", length = 100)
	private String specialization;

	@Size(max = 2000)
	@Column(name = "biography", length = 2000)
	private String biography;

	/** Owning side: a profile cannot exist without its technician. */
	@OneToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "technician_id", nullable = false, unique = true,
			foreignKey = @ForeignKey(name = "fk_technician_profile_technician"))
	private Technician technician;

	protected TechnicianProfile() {
		// Required by JPA
	}

	public TechnicianProfile(String phone, String specialization, String biography) {
		this.phone = phone;
		this.specialization = specialization;
		this.biography = biography;
	}

	/** Reserved for {@link Technician}, which keeps both sides synchronized. */
	protected void linkTo(Technician technician) {
		this.technician = technician;
	}

	public Long getId() {
		return this.id;
	}

	public String getPhone() {
		return this.phone;
	}

	public void setPhone(String phone) {
		this.phone = phone;
	}

	public String getSpecialization() {
		return this.specialization;
	}

	public void setSpecialization(String specialization) {
		this.specialization = specialization;
	}

	public String getBiography() {
		return this.biography;
	}

	public void setBiography(String biography) {
		this.biography = biography;
	}

	public Technician getTechnician() {
		return this.technician;
	}

	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		return other instanceof TechnicianProfile that && this.id != null && this.id.equals(that.getId());
	}

	@Override
	public int hashCode() {
		return TechnicianProfile.class.hashCode();
	}

	@Override
	public String toString() {
		return "TechnicianProfile{id=" + this.id + ", specialization='" + this.specialization + "'}";
	}

}
