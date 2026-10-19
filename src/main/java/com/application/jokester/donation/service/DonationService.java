package com.application.jokester.donation.service;

import com.application.jokester.donation.dto.*;
import org.springframework.data.domain.Page;

import java.util.UUID;

public interface DonationService {

    RazorpayOrderResponse createOrder(CreateDonationRequest request, UUID userId);

    DonationResponse verifyAndSavePayment(VerifyPaymentRequest request, UUID userId);

    Page<DonationResponse> getMyDonations(UUID userId, int page, int size);
}
