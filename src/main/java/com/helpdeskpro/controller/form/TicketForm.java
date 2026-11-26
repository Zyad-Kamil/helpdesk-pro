package com.helpdeskpro.controller.form;

import com.helpdeskpro.dto.TicketRequest;
import com.helpdeskpro.dto.TicketResponse;
import com.helpdeskpro.entity.TicketPriority;

public class TicketForm {

	private String title;

	private String description;

	private String requesterName;

	private String requesterEmail;

	private TicketPriority priority = TicketPriority.MEDIUM;

	public static TicketForm from(TicketResponse ticket) {
		TicketForm form = new TicketForm();
		form.title = ticket.title();
		form.description = ticket.description();
		form.requesterName = ticket.requesterName();
		form.requesterEmail = ticket.requesterEmail();
		form.priority = ticket.priority();
		return form;
	}

	public TicketRequest toRequest() {
		return new TicketRequest(this.title, this.description, this.requesterName, this.requesterEmail,
				this.priority);
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

	public TicketPriority getPriority() {
		return this.priority;
	}

	public void setPriority(TicketPriority priority) {
		this.priority = priority;
	}

}
