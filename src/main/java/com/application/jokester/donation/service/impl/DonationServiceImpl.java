package com.application.jokester.donation.service.impl;

import com.application.jokester.auth.entity.User;
import com.application.jokester.auth.repository.UserRepository;
import com.application.jokester.donation.dto.*;
import com.application.jokester.donation.entity.Donation;
import com.application.jokester.donation.repository.DonationRepository;
import com.application.jokester.donation.service.DonationService;
import com.application.jokester.event.DonationEvent;
import com.application.jokester.event.DonationEventProducer;
import com.application.jokester.exception.ExternalApiException;
import com.application.jokester.exception.ResourceNotFoundException;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.razorpay.Utils;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class DonationServiceImpl implements DonationService {

    private final RazorpayClient razorpayClient;
    @Qualifier("razorpayKeyId")
    private final String razorpayKeyId;
    @Qualifier("razorpayKeySecret")
    private final String razorpayKeySecret;
    private final DonationRepository donationRepository;
    private final UserRepository userRepository;
    private final DonationEventProducer donationEventProducer;

    @Override
    @Transactional
    @CircuitBreaker(name = "razorpay", fallbackMethod = "createOrderFallback")
    public RazorpayOrderResponse createOrder(CreateDonationRequest request, UUID userId) {
        try {
            // Razorpay expects amount in paise (1 INR = 100 paise)
            int amountInPaise = request.getAmount().multiply(BigDecimal.valueOf(100)).intValue();

            JSONObject orderRequest = new JSONObject();
            orderRequest.put("amount", amountInPaise);
            orderRequest.put("currency", request.getCurrency());
            orderRequest.put("receipt", "donation_" + UUID.randomUUID());

            Order razorpayOrder = razorpayClient.orders.create(orderRequest);

            User userRef = userRepository.getReferenceById(userId);

            Donation donation = Donation.builder()
                    .user(userRef)
                    .amount(request.getAmount())
                    .currency(request.getCurrency())
                    .razorpayOrderId(razorpayOrder.get("id"))
                    .status("CREATED")
                    .build();

            donationRepository.save(donation);

            return RazorpayOrderResponse.builder()
                    .orderId(razorpayOrder.get("id"))
                    .amount(request.getAmount())
                    .currency(request.getCurrency())
                    .keyId(razorpayKeyId)
                    .build();

        } catch (RazorpayException e) {
            log.error("Razorpay order creation failed: {}", e.getMessage());
            throw new ExternalApiException("Payment service is temporarily unavailable. Please try again.");
        }
    }

    @SuppressWarnings("unused")
    private RazorpayOrderResponse createOrderFallback(CreateDonationRequest request, UUID userId, Throwable t) {
        log.warn("Circuit breaker triggered for Razorpay: {}", t.getMessage());
        throw new ExternalApiException(
                "Payment service is currently experiencing issues. Please try again in a few minutes.");
    }

    @Override
    @Transactional
    @CacheEvict(value = "donations", allEntries = true)
    public DonationResponse verifyAndSavePayment(VerifyPaymentRequest request, UUID userId) {
        Donation donation = donationRepository
                .findByRazorpayOrderId(request.getRazorpayOrderId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Donation order not found: " + request.getRazorpayOrderId()));

        // Verify Razorpay payment signature using HMAC-SHA256
        try {
            JSONObject options = new JSONObject();
            options.put("razorpay_order_id", request.getRazorpayOrderId());
            options.put("razorpay_payment_id", request.getRazorpayPaymentId());
            options.put("razorpay_signature", request.getRazorpaySignature());

            boolean isValid = Utils.verifyPaymentSignature(options, razorpayKeySecret);

            if (!isValid) {
                donation.setStatus("FAILED");
                donationRepository.save(donation);
                throw new ExternalApiException("Payment verification failed. Invalid signature.");
            }
        } catch (RazorpayException e) {
            donation.setStatus("FAILED");
            donationRepository.save(donation);
            throw new ExternalApiException("Payment verification error: " + e.getMessage());
        }

        // Mark as paid
        donation.setRazorpayPaymentId(request.getRazorpayPaymentId());
        donation.setStatus("PAID");
        donationRepository.save(donation);

        // Publish Kafka event
        donationEventProducer.publishDonationEvent(DonationEvent.builder()
                .donationId(donation.getId())
                .username(donation.getUser().getUsername())
                .amount(donation.getAmount())
                .currency(donation.getCurrency())
                .paymentId(donation.getRazorpayPaymentId())
                .timestamp(LocalDateTime.now())
                .build());

        return toResponse(donation);
    }

    @Override
    @Cacheable(value = "donations", key = "#userId + '-' + #page + '-' + #size")
    public Page<DonationResponse> getMyDonations(UUID userId, int page, int size) {
        return donationRepository
                .findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(page, size))
                .map(this::toResponse);
    }

    private DonationResponse toResponse(Donation donation) {
        return DonationResponse.builder()
                .id(donation.getId())
                .donorUsername(donation.getUser().getUsername())
                .amount(donation.getAmount())
                .currency(donation.getCurrency())
                .razorpayOrderId(donation.getRazorpayOrderId())
                .razorpayPaymentId(donation.getRazorpayPaymentId())
                .status(donation.getStatus())
                .createdAt(donation.getCreatedAt())
                .build();
    }
}
