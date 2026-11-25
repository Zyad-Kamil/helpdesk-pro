package com.helpdeskpro.entity;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Entity
@Table(name = "technician", uniqueConstraints = @UniqueConstraint(name = "uk_technician_email", columnNames = "email"))
public class Technician {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@NotBlank
	@Size(max = 80)
	@Column(name = "first_name", nullable = false, length = 80)
	private String firstName;

	@NotBlank
	@Size(max = 80)
	@Column(name = "last_name", nullable = false, length = 80)
	private String lastName;

	@NotBlank
	@Email
	@Size(max = 255)
	@Column(name = "email", nullable = false, length = 255)
	private String email;

	@Column(name = "active", nullable = false)
	private boolean active = true;

	@OneToOne(mappedBy = "technician", cascade = CascadeType.ALL, orphanRemoval = true)
	private TechnicianProfile profile;

	@OneToMany(mappedBy = "technician")
	private Set<Ticket> tickets = new HashSet<>();

	protected Technician() {
		// Required by JPA
	}

	public Technician(String firstName, String lastName, String email) {
		this.firstName = firstName;
		this.lastName = lastName;
		this.email = email;
	}

	/**
	 * Links a new profile to this technician and updates both sides of the relationship.
	 * An existing profile must be edited or removed first: Hibernate inserts before it
	 * deletes, so replacing it in a single flush would break the unique foreign key.
	 */
	public void assignProfile(TechnicianProfile newProfile) {
		if (this.profile != null) {
			throw new IllegalStateException("The technician already has a profile");
		}
		if (newProfile.getTechnician() != null) {
			throw new IllegalStateException("The profile already belongs to a technician");
		}
		this.profile = newProfile;
		newProfile.linkTo(this);
	}

	/** Unlinks the profile, which is then deleted by orphan removal. */
	public void removeProfile() {
		if (this.profile != null) {
			this.profile.linkTo(null);
			this.profile = null;
		}
	}

	/** Assigns the ticket to this technician and updates both sides of the relationship. */
	public void addTicket(Ticket ticket) {
		ticket.assignTechnician(this);
	}

	/** Unassigns the ticket if it belongs to this technician; other tickets are left untouched. */
	public void removeTicket(Ticket ticket) {
		if (equals(ticket.getTechnician())) {
			ticket.assignTechnician(null);
		}
	}

	/** Mutable inverse side, reserved for {@link Ticket}, which owns the relationship. */
	protected Set<Ticket> ticketsInternal() {
		return this.tickets;
	}

	public Long getId() {
		return this.id;
	}

	public String getFirstName() {
		return this.firstName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public String getLastName() {
		return this.lastName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	public String getEmail() {
		return this.email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public boolean isActive() {
		return this.active;
	}

	public void setActive(boolean active) {
		this.active = active;
	}

	public TechnicianProfile getProfile() {
		return this.profile;
	}

	public Set<Ticket> getTickets() {
		return Collections.unmodifiableSet(this.tickets);
	}

	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		return other instanceof Technician that && this.id != null && this.id.equals(that.getId());
	}

	@Override
	public int hashCode() {
		return Technician.class.hashCode();
	}

	@Override
	public String toString() {
		return "Technician{id=" + this.id + ", active=" + this.active + "}";
	}

}
