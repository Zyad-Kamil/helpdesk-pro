package com.helpdeskpro.dto;

import com.helpdeskpro.entity.TicketStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Requested status change.
 *
 * @param resolution mandatory (not blank) when the target status is {@code RESOLVED}, ignored otherwise
 */
public record TicketStatusRequest(@NotNull TicketStatus status, @Size(max = 4000) String resolution) {

}
