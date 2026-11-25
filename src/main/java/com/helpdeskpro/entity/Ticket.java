package com.helpdeskpro.entity;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "ticket")
public class Ticket {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@NotBlank
	@Size(max = 150)
	@Column(name = "title", nullable = false, length = 150)
	private String title;

	@NotBlank
	@Size(max = 4000)
	@Column(name = "description", nullable = false, length = 4000)
	private String description;

	@NotBlank
	@Size(max = 160)
	@Column(name = "requester_name", nullable = false, length = 160)
	private String requesterName;

	@NotBlank
	@Email
	@Size(max = 255)
	@Column(name = "requester_email", nullable = false, length = 255)
	private String requesterEmail;

	/** VARCHAR is explicit: Hibernate would otherwise create a native H2 ENUM column. */
	@NotNull
	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.VARCHAR)
	@Column(name = "status", nullable = false, length = 20)
	private TicketStatus status = TicketStatus.OPEN;

	@NotNull
	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.VARCHAR)
	@Column(name = "priority", nullable = false, length = 20)
	private TicketPriority priority = TicketPriority.MEDIUM;

	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	@Column(name = "updated_at", nullable = false)
	private LocalDateTime updatedAt;

	@Column(name = "resolved_at")
	private LocalDateTime resolvedAt;

	@Size(max = 4000)
	@Column(name = "resolution", length = 4000)
	private String resolution;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "technician_id", foreignKey = @ForeignKey(name = "fk_ticket_technician"))
	private Technician technician;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "category_id", foreignKey = @ForeignKey(name = "fk_ticket_category"))
	private Category category;

	@ManyToMany
	@JoinTable(name = "ticket_tag",
			joinColumns = @JoinColumn(name = "ticket_id", nullable = false),
			foreignKey = @ForeignKey(name = "fk_ticket_tag_ticket"),
			inverseJoinColumns = @JoinColumn(name = "tag_id", nullable = false),
			inverseForeignKey = @ForeignKey(name = "fk_ticket_tag_tag"))
	private Set<Tag> tags = new HashSet<>();

	protected Ticket() {
		// Required by JPA
	}

	public Ticket(String title, String description, String requesterName, String requesterEmail) {
		this.title = title;
		this.description = description;
		this.requesterName = requesterName;
		this.requesterEmail = requesterEmail;
	}

	@PrePersist
	void onCreate() {
		this.createdAt = now();
		this.updatedAt = this.createdAt;
	}

	@PreUpdate
	void onUpdate() {
		this.updatedAt = now();
	}

	/** Truncated to the precision of the database column so that values survive a reload. */
	private static LocalDateTime now() {
		return LocalDateTime.now().truncatedTo(ChronoUnit.MICROS);
	}

	/** Assigns the technician, or unassigns the current one when {@code null}; syncs both sides. */
	public void assignTechnician(Technician newTechnician) {
		if (this.technician != null) {
			this.technician.ticketsInternal().remove(this);
		}
		this.technician = newTechnician;
		if (newTechnician != null) {
			newTechnician.ticketsInternal().add(this);
		}
	}

	/** Assigns the category, or unassigns the current one when {@code null}; syncs both sides. */
	public void assignCategory(Category newCategory) {
		if (this.category != null) {
			this.category.ticketsInternal().remove(this);
		}
		this.category = newCategory;
		if (newCategory != null) {
			newCategory.ticketsInternal().add(this);
		}
	}

	/** Links the tag and updates both sides of the relationship. */
	public void addTag(Tag tag) {
		this.tags.add(tag);
		tag.ticketsInternal().add(this);
	}

	/** Unlinks the tag and updates both sides of the relationship; the tag itself is kept. */
	public void removeTag(Tag tag) {
		this.tags.remove(tag);
		tag.ticketsInternal().remove(this);
	}

	public Long getId() {
		return this.id;
	}

	public String getTitle() {
		return this.title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getDescription() {
		return this.description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getRequesterName() {
		return this.requesterName;
	}

	public void setRequesterName(String requesterName) {
		this.requesterName = requesterName;
	}

	public String getRequesterEmail() {
		return this.requesterEmail;
	}

	public void setRequesterEmail(String requesterEmail) {
		this.requesterEmail = requesterEmail;
	}

	public TicketStatus getStatus() {
		return this.status;
	}

	public void setStatus(TicketStatus status) {
		this.status = status;
	}

	public TicketPriority getPriority() {
		return this.priority;
	}

	public void setPriority(TicketPriority priority) {
		this.priority = priority;
	}

	public LocalDateTime getCreatedAt() {
		return this.createdAt;
	}

	public LocalDateTime getUpdatedAt() {
		return this.updatedAt;
	}

	public LocalDateTime getResolvedAt() {
		return this.resolvedAt;
	}

	public void setResolvedAt(LocalDateTime resolvedAt) {
		this.resolvedAt = resolvedAt;
	}

	public String getResolution() {
		return this.resolution;
	}

	public void setResolution(String resolution) {
		this.resolution = resolution;
	}

	public Technician getTechnician() {
		return this.technician;
	}

	public Category getCategory() {
		return this.category;
	}

	public Set<Tag> getTags() {
		return Collections.unmodifiableSet(this.tags);
	}

	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		return other instanceof Ticket that && this.id != null && this.id.equals(that.getId());
	}

	@Override
	public int hashCode() {
		return Ticket.class.hashCode();
	}

	@Override
	public String toString() {
		return "Ticket{id=" + this.id + ", status=" + this.status + ", priority=" + this.priority + "}";
	}

}
