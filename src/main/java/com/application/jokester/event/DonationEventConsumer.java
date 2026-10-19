package com.application.jokester.event;

import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class DonationEventConsumer {

    /**
     * Listens for donation events from the donation-events topic.
     * This consumer handles post-payment processing such as logging,
     * analytics, and future notification integrations.
     *
     * To add email notifications, integrate an email service here.
     * To build a donor leaderboard, aggregate donation data here.
     */
    @KafkaListener(
            topics = "donation-events",
            groupId = "jokester-group"
    )
    public void handleDonationEvent(DonationEvent event) {
        log.info("===== DONATION RECEIVED =====");
        log.info("Donor      : {}", event.getUsername());
        log.info("Amount     : {} {}", event.getAmount(), event.getCurrency());
        log.info("Payment ID : {}", event.getPaymentId());
        log.info("Donation ID: {}", event.getDonationId());
        log.info("Timestamp  : {}", event.getTimestamp());
        log.info("=============================");

        // Future extensions:
        // 1. Send thank-you email to donor
        // 2. Update real-time donor leaderboard
        // 3. Trigger achievement/badge system
        // 4. Push notification to admin dashboard
    }
}
