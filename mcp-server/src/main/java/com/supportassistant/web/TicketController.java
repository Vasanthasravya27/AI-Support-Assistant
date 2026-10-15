package com.supportassistant.web;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.supportassistant.ticket.CreateTicketRequest;
import com.supportassistant.ticket.TicketService;
import com.supportassistant.ticket.TicketStatus;
import com.supportassistant.ticket.TicketView;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/** Plain REST API over the same TicketService the MCP tools use. Handy for testing and admin UIs. */
@RestController
@RequestMapping("/api/tickets")
public class TicketController {

    private final TicketService ticketService;

    public TicketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TicketView create(@Valid @RequestBody CreateTicketRequest request) {
        return ticketService.create(request);
    }

    @GetMapping("/{ticketNumber}")
    public TicketView get(@PathVariable String ticketNumber) {
        return ticketService.getByNumber(ticketNumber);
    }

    @GetMapping
    public List<TicketView> listByCustomer(@RequestParam String email) {
        return ticketService.listByCustomer(email);
    }

    @PatchMapping("/{ticketNumber}/status")
    public TicketView updateStatus(@PathVariable String ticketNumber,
                                   @Valid @RequestBody UpdateStatusRequest request) {
        return ticketService.updateStatus(ticketNumber, request.status());
    }

    public record UpdateStatusRequest(@NotNull TicketStatus status) {
    }
}
