package xyz.imaketech.holdmyseat.booking.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class HoldExpirySweeper {

    private static final Logger log = LoggerFactory.getLogger(HoldExpirySweeper.class);

    private final BookingService bookings;

    public HoldExpirySweeper(BookingService bookings) {
        this.bookings = bookings;
    }

    @Scheduled(fixedDelayString = "${holdmyseat.booking.sweep-ms:15000}")
    public void sweep() {
        int expired = bookings.expireOverdue();
        if (expired > 0) {
            log.info("Expired {} overdue bookings", expired);
        }
    }
}
