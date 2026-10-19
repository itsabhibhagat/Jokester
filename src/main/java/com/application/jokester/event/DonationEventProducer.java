package com.application.jokester.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Slf4j
@Component
@RequiredArgsConstructor
public class DonationEventProducer {

    private static final String TOPIC = "donation-events";

    private final KafkaTemplate<String, DonationEvent> kafkaTemplate;

    public void publishDonationEvent(DonationEvent event) {
        CompletableFuture<SendResult<String, DonationEvent>> future =
                kafkaTemplate.send(TOPIC, event.getDonationId().toString(), event);

        future.whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("Failed to publish donation event for donation {}: {}",
                        event.getDonationId(), ex.getMessage());
            } else {
                log.info("Donation event published — donationId={}, amount={} {}, topic={}, partition={}, offset={}",
                        event.getDonationId(),
                        event.getAmount(),
                        event.getCurrency(),
                        result.getRecordMetadata().topic(),
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
            }
        });
    }
}
