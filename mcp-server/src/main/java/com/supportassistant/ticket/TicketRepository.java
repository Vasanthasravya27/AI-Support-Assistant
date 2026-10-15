package com.supportassistant.ticket;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TicketRepository extends JpaRepository<Ticket, Long> {

    List<Ticket> findByCustomerEmailOrderByCreatedAtDesc(String customerEmail);
}
