package com.application.jokester.donation.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class RazorpayOrderResponse {

    private String orderId;
    private BigDecimal amount;
    private String currency;
    private String keyId;
}
