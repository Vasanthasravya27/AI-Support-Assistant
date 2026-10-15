package com.supportassistant.ticket;

import java.util.Locale;

public enum TicketPriority {
    LOW,
    MEDIUM,
    HIGH,
    URGENT;

    /** Lenient parser used by the MCP tools (LLMs may send "high", "High", or nothing). */
    public static TicketPriority parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return MEDIUM;
        }
        try {
            return valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "Invalid priority '" + raw + "'. Use one of: LOW, MEDIUM, HIGH, URGENT");
        }
    }
}
