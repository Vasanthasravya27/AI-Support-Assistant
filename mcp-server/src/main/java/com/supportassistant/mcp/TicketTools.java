package com.supportassistant.mcp;

import java.util.List;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import com.supportassistant.ticket.CreateTicketRequest;
import com.supportassistant.ticket.TicketPriority;
import com.supportassistant.ticket.TicketService;
import com.supportassistant.ticket.TicketView;

/**
 * MCP tools for ticketing. Exceptions thrown here are reported to the MCP client as tool errors,
 * so the model sees the message and can correct itself (e.g. a malformed ticket number).
 */
@Component
public class TicketTools {

    private final TicketService ticketService;

    public TicketTools(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    @Tool(name = "create_support_ticket",
            description = """
                    Creates a support ticket for a human agent. Use ONLY when the knowledge base cannot \
                    resolve the issue or the customer explicitly asks for a human. Requires the customer's \
                    email address; ask for it if you do not have it. Returns the new ticket number.""")
    public TicketView createSupportTicket(
            @ToolParam(description = "Customer's email address") String customerEmail,
            @ToolParam(description = "Short summary of the problem (max 200 characters)") String subject,
            @ToolParam(description = "Detailed description including what the customer already tried") String description,
            @ToolParam(description = "One of LOW, MEDIUM, HIGH, URGENT. Defaults to MEDIUM.",
                    required = false) String priority) {

        return ticketService.create(new CreateTicketRequest(
                customerEmail, subject, description, TicketPriority.parse(priority)));
    }

    @Tool(name = "get_ticket_status",
            description = "Looks up a support ticket by its number (format TCK-00042) and returns its current status.")
    public TicketView getTicketStatus(
            @ToolParam(description = "Ticket number, e.g. TCK-00042") String ticketNumber) {
        return ticketService.getByNumber(ticketNumber);
    }

    @Tool(name = "list_customer_tickets",
            description = "Lists all support tickets opened by a customer, newest first.")
    public List<TicketView> listCustomerTickets(
            @ToolParam(description = "Customer's email address") String customerEmail) {
        return ticketService.listByCustomer(customerEmail);
    }
}
