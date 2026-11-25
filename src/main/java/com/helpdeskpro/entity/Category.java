package com.helpdeskpro.entity;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Entity
@Table(name = "category", uniqueConstraints = @UniqueConstraint(name = "uk_category_name", columnNames = "name"))
public class Category {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@NotBlank
	@Size(max = 80)
	@Column(name = "name", nullable = false, length = 80)
	private String name;

	@Size(max = 500)
	@Column(name = "description", length = 500)
	private String description;

	@OneToMany(mappedBy = "category")
	private Set<Ticket> tickets = new HashSet<>();

	protected Category() {
		// Required by JPA
	}

	public Category(String name, String description) {
		this.name = name;
		this.description = description;
	}

	/** Links the ticket to this category and updates both sides of the relationship. */
	public void addTicket(Ticket ticket) {
		ticket.assignCategory(this);
	}

	/** Unlinks the ticket if it belongs to this category; other tickets are left untouched. */
	public void removeTicket(Ticket ticket) {
		if (equals(ticket.getCategory())) {
			ticket.assignCategory(null);
		}
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

	public String getDescription() {
		return this.description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public Set<Ticket> getTickets() {
		return Collections.unmodifiableSet(this.tickets);
	}

	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		return other instanceof Category that && this.id != null && this.id.equals(that.getId());
	}

	@Override
	public int hashCode() {
		return Category.class.hashCode();
	}

	@Override
	public String toString() {
		return "Category{id=" + this.id + ", name='" + this.name + "'}";
	}

}
