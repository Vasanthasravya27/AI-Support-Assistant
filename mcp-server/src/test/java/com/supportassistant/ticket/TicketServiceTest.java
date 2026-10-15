package com.supportassistant.ticket;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import jakarta.validation.ConstraintViolationException;

/** Full-context test: also proves the whole app (JPA, MCP server, tools) wires up. */
@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:tickets-test;DB_CLOSE_DELAY=-1")
@Transactional
class TicketServiceTest {

    @Autowired
    TicketService service;

    @Test
    void createsTicketWithDefaultsAndNormalizedEmail() {
        TicketView created = service.create(
                new CreateTicketRequest("  Ada@Example.COM ", "Cannot log in", "2FA code rejected", null));

        assertThat(created.ticketNumber()).matches("TCK-\\d{5}");
        assertThat(created.customerEmail()).isEqualTo("ada@example.com");
        assertThat(created.status()).isEqualTo(TicketStatus.OPEN);
        assertThat(created.priority()).isEqualTo(TicketPriority.MEDIUM);
    }

    @Test
    void looksUpTicketByNumberCaseInsensitively() {
        TicketView created = service.create(
                new CreateTicketRequest("bob@example.com", "Refund", "Charged twice", TicketPriority.HIGH));

        TicketView found = service.getByNumber(created.ticketNumber().toLowerCase());

        assertThat(found.subject()).isEqualTo("Refund");
        assertThat(found.priority()).isEqualTo(TicketPriority.HIGH);
    }

    @Test
    void listsTicketsForCustomer() {
        service.create(new CreateTicketRequest("cy@example.com", "First", "one", null));
        service.create(new CreateTicketRequest("cy@example.com", "Second", "two", null));

        assertThat(service.listByCustomer("CY@example.com")).hasSize(2);
        assertThat(service.listByCustomer("nobody@example.com")).isEmpty();
    }

    @Test
    void updatesStatus() {
        TicketView created = service.create(new CreateTicketRequest("di@example.com", "Bug", "details", null));

        TicketView updated = service.updateStatus(created.ticketNumber(), TicketStatus.RESOLVED);

        assertThat(updated.status()).isEqualTo(TicketStatus.RESOLVED);
    }

    @Test
    void rejectsInvalidInput() {
        assertThatThrownBy(() -> service.create(new CreateTicketRequest("not-an-email", "x", "y", null)))
                .isInstanceOf(ConstraintViolationException.class);
        assertThatThrownBy(() -> service.getByNumber("42"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.getByNumber("TCK-99999"))
                .isInstanceOf(TicketNotFoundException.class);
    }
}
