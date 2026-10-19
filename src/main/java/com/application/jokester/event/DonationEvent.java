package com.application.jokester.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DonationEvent {

    private UUID donationId;
    private String username;
    private BigDecimal amount;
    private String currency;
    private String paymentId;
    private LocalDateTime timestamp;
}
