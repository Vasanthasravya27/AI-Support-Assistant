package com.supportassistant.ticket;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateTicketRequest(
        @NotBlank @Email @Size(max = 254) String customerEmail,
        @NotBlank @Size(max = 200) String subject,
        @NotBlank @Size(max = 4000) String description,
        TicketPriority priority) {
}
