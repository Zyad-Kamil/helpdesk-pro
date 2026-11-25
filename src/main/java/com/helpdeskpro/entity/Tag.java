package com.helpdeskpro.entity;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Entity
@Table(name = "tag", uniqueConstraints = @UniqueConstraint(name = "uk_tag_name", columnNames = "name"))
public class Tag {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@NotBlank
	@Size(max = 50)
	@Column(name = "name", nullable = false, length = 50)
	private String name;

	@ManyToMany(mappedBy = "tags")
	private Set<Ticket> tickets = new HashSet<>();

	protected Tag() {
		// Required by JPA
	}

	public Tag(String name) {
		this.name = name;
	}

	/** Mutable inverse side, reserved for {@link Ticket}, which owns the relationship. */
	protected Set<Ticket> ticketsInternal() {
		return this.tickets;
	}

	public Long getId() {
		return this.id;
	}

	public String getName() {
		return this.name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Set<Ticket> getTickets() {
		return Collections.unmodifiableSet(this.tickets);
	}

	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		return other instanceof Tag that && this.id != null && this.id.equals(that.getId());
	}

	@Override
	public int hashCode() {
		return Tag.class.hashCode();
	}

	@Override
	public String toString() {
		return "Tag{id=" + this.id + ", name='" + this.name + "'}";
	}

}
