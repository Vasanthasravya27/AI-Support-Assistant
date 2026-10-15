package com.supportassistant.ticket;

public class TicketNotFoundException extends RuntimeException {

    public TicketNotFoundException(String ticketNumber) {
        super("Ticket " + ticketNumber + " was not found");
    }
}
