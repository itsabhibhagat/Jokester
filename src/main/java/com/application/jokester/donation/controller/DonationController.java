package com.application.jokester.donation.controller;

import com.application.jokester.common.ApiResponse;
import com.application.jokester.config.TokenPrincipal;
import com.application.jokester.donation.dto.*;
import com.application.jokester.donation.service.DonationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/donations")
@RequiredArgsConstructor
@Tag(name = "Donations", description = "Endpoints for making donations via Razorpay")
public class DonationController {

    private final DonationService donationService;

    @Operation(
            summary = "Create a donation order",
            description = "Creates a Razorpay order for the specified amount. Returns order details needed by the frontend to launch Razorpay Checkout."
    )
    @PostMapping("/create-order")
    public ResponseEntity<ApiResponse<RazorpayOrderResponse>> createOrder(
            @Valid @RequestBody CreateDonationRequest request,
            @AuthenticationPrincipal TokenPrincipal principal) {

        RazorpayOrderResponse response = donationService.createOrder(request, principal.userId());
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Razorpay order created successfully", response));
    }

    @Operation(
            summary = "Verify payment and complete donation",
            description = "Verifies the Razorpay payment signature (HMAC-SHA256) and marks the donation as PAID. Publishes a Kafka event on success."
    )
    @PostMapping("/verify")
    public ResponseEntity<ApiResponse<DonationResponse>> verifyPayment(
            @Valid @RequestBody VerifyPaymentRequest request,
            @AuthenticationPrincipal TokenPrincipal principal) {

        DonationResponse response = donationService.verifyAndSavePayment(request, principal.userId());
        return ResponseEntity.ok(
                ApiResponse.success("Payment verified and donation recorded", response));
    }

    @Operation(
            summary = "Get my donation history",
            description = "Returns the authenticated user's donation history, sorted by most recent. Paginated."
    )
    @GetMapping("/my")
    public ResponseEntity<ApiResponse<Page<DonationResponse>>> getMyDonations(
            @AuthenticationPrincipal TokenPrincipal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Page<DonationResponse> donations = donationService.getMyDonations(
                principal.userId(), page, size);
        return ResponseEntity.ok(
                ApiResponse.success("Donation history retrieved", donations));
    }
}
