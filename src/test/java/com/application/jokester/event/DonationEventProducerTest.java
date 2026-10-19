package com.application.jokester.event;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class DonationEventProducerTest {

    @Mock
    private KafkaTemplate<String, DonationEvent> kafkaTemplate;

    @InjectMocks
    private DonationEventProducer donationEventProducer;

    @Captor
    private ArgumentCaptor<DonationEvent> eventCaptor;

    @Test
    void should_PublishEvent_When_Called() {
        UUID donationId = UUID.randomUUID();
        DonationEvent event = DonationEvent.builder()
                .donationId(donationId)
                .amount(BigDecimal.TEN)
                .currency("INR")
                .username("testuser")
                .build();

        CompletableFuture<SendResult<String, DonationEvent>> future = new CompletableFuture<>();
        when(kafkaTemplate.send(eq("donation-events"), eq(donationId.toString()), eventCaptor.capture()))
                .thenReturn(future);

        donationEventProducer.publishDonationEvent(event);

        verify(kafkaTemplate).send(eq("donation-events"), eq(donationId.toString()), eventCaptor.capture());
        DonationEvent capturedEvent = eventCaptor.getValue();
        assertThat(capturedEvent.getDonationId()).isEqualTo(donationId);
        assertThat(capturedEvent.getAmount()).isEqualTo(BigDecimal.TEN);
    }
}
