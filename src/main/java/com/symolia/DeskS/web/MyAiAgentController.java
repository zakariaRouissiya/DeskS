package com.symolia.DeskS.web;

import com.symolia.DeskS.entities.Ticket;
import com.symolia.DeskS.services.TicketService;
import com.symolia.DeskS.services.TicketAiService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class MyAiAgentController {
    private final TicketService ticketService;
    private final TicketAiService ticketAiService;

    @GetMapping(value = "/myaiagent/{ticketId}", produces = MediaType.TEXT_MARKDOWN_VALUE)
    public String analyseTicket(@PathVariable Long ticketId) {
        Ticket ticket = ticketService.getTicketWithDetails(ticketId);
        return ticketAiService.analyseTicket(ticket);
    }
}