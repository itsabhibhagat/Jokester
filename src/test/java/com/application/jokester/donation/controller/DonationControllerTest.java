package com.application.jokester.donation.controller;

import com.application.jokester.config.JwtService;
import com.application.jokester.config.TokenPrincipal;
import com.application.jokester.config.UserDetailsServiceImpl;
import com.application.jokester.donation.dto.CreateDonationRequest;
import com.application.jokester.donation.dto.DonationResponse;
import com.application.jokester.donation.dto.RazorpayOrderResponse;
import com.application.jokester.donation.dto.VerifyPaymentRequest;
import com.application.jokester.donation.service.DonationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DonationController.class)
@AutoConfigureMockMvc(addFilters = false)
public class DonationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DonationService donationService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private UserDetailsServiceImpl userDetailsService;

    @MockBean
    private LettuceBasedProxyManager<String> proxyManager;

    @Autowired
    private ObjectMapper objectMapper;

    private UUID testUserId;

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();
        TokenPrincipal mockPrincipal = new TokenPrincipal(testUserId, "testuser");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(mockPrincipal, null, List.of()));
    }

    @Test
    void should_Return201_When_CreateOrderSuccess() throws Exception {
        CreateDonationRequest request = new CreateDonationRequest();
        request.setAmount(BigDecimal.TEN);
        request.setCurrency("INR");

        RazorpayOrderResponse response = RazorpayOrderResponse.builder()
                .orderId("order_123")
                .amount(BigDecimal.TEN)
                .currency("INR")
                .keyId("rzp_test_key")
                .build();

        when(donationService.createOrder(any(CreateDonationRequest.class), eq(testUserId)))
                .thenReturn(response);

        mockMvc.perform(post("/api/donations/create-order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.orderId").value("order_123"))
                .andExpect(jsonPath("$.data.amount").value(10))
                .andExpect(jsonPath("$.message").value("Razorpay order created successfully"));
    }

    @Test
    void should_Return200_When_VerifyPaymentSuccess() throws Exception {
        VerifyPaymentRequest request = new VerifyPaymentRequest();
        request.setRazorpayOrderId("order_123");
        request.setRazorpayPaymentId("pay_123");
        request.setRazorpaySignature("valid_sig");

        DonationResponse response = DonationResponse.builder()
                .razorpayOrderId("order_123")
                .razorpayPaymentId("pay_123")
                .status("PAID")
                .build();

        when(donationService.verifyAndSavePayment(any(VerifyPaymentRequest.class), eq(testUserId)))
                .thenReturn(response);

        mockMvc.perform(post("/api/donations/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PAID"))
                .andExpect(jsonPath("$.message").value("Payment verified and donation recorded"));
    }

    @Test
    void should_Return200_When_GetMyDonations() throws Exception {
        DonationResponse response = DonationResponse.builder()
                .donorUsername("testuser")
                .amount(BigDecimal.TEN)
                .status("PAID")
                .build();

        when(donationService.getMyDonations(eq(testUserId), eq(0), eq(10)))
                .thenReturn(new PageImpl<>(List.of(response)));

        mockMvc.perform(get("/api/donations/my")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].donorUsername").value("testuser"))
                .andExpect(jsonPath("$.data.content[0].status").value("PAID"));
    }
}
