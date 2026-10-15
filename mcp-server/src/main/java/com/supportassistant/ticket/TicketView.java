package com.supportassistant.ticket;

/**
 * Read-only projection of a ticket. Timestamps are ISO-8601 strings so the shape is
 * identical in REST responses and in MCP tool results (which an LLM will read).
 */
public record TicketView(
        String ticketNumber,
        String customerEmail,
        String subject,
        TicketStatus status,
        TicketPriority priority,
        String createdAt,
        String updatedAt) {

    public static TicketView from(Ticket t) {
        return new TicketView(
                t.getTicketNumber(),
                t.getCustomerEmail(),
                t.getSubject(),
                t.getStatus(),
                t.getPriority(),
                t.getCreatedAt().toString(),
                t.getUpdatedAt().toString());
    }
}
