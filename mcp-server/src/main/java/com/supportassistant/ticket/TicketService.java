package com.supportassistant.ticket;

import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.Valid;

/** Business logic for support tickets. Used by both the REST API and the MCP tools. */
@Service
@Validated
@Transactional
public class TicketService {

    private static final Pattern TICKET_NUMBER = Pattern.compile("(?i)^TCK-(\\d{1,18})$");

    private final TicketRepository repository;

    public TicketService(TicketRepository repository) {
        this.repository = repository;
    }

    public TicketView create(@Valid CreateTicketRequest request) {
        TicketPriority priority = request.priority() == null ? TicketPriority.MEDIUM : request.priority();
        Ticket saved = repository.save(new Ticket(
                normalizeEmail(request.customerEmail()),
                request.subject().trim(),
                request.description().trim(),
                priority));
        return TicketView.from(saved);
    }

    @Transactional(readOnly = true)
    public TicketView getByNumber(String ticketNumber) {
        return TicketView.from(find(ticketNumber));
    }

    @Transactional(readOnly = true)
    public List<TicketView> listByCustomer(String customerEmail) {
        if (customerEmail == null || customerEmail.isBlank()) {
            throw new IllegalArgumentException("customerEmail must not be empty");
        }
        return repository.findByCustomerEmailOrderByCreatedAtDesc(normalizeEmail(customerEmail))
                .stream()
                .map(TicketView::from)
                .toList();
    }

    /** Intentionally NOT exposed as an MCP tool: status changes are for human agents. */
    public TicketView updateStatus(String ticketNumber, TicketStatus newStatus) {
        if (newStatus == null) {
            throw new IllegalArgumentException("status must not be null");
        }
        Ticket ticket = find(ticketNumber);
        ticket.changeStatus(newStatus);
        return TicketView.from(ticket);
    }

    private Ticket find(String ticketNumber) {
        Matcher matcher = TICKET_NUMBER.matcher(ticketNumber == null ? "" : ticketNumber.trim());
        if (!matcher.matches()) {
            throw new IllegalArgumentException(
                    "Invalid ticket number '" + ticketNumber + "'. Expected a format like TCK-00042");
        }
        return repository.findById(Long.parseLong(matcher.group(1)))
                .orElseThrow(() -> new TicketNotFoundException(ticketNumber.trim().toUpperCase(Locale.ROOT)));
    }

    private static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
