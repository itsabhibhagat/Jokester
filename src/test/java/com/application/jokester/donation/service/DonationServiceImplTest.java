package com.application.jokester.donation.service;

import com.application.jokester.auth.entity.User;
import com.application.jokester.auth.repository.UserRepository;
import com.application.jokester.donation.dto.CreateDonationRequest;
import com.application.jokester.donation.dto.DonationResponse;
import com.application.jokester.donation.dto.RazorpayOrderResponse;
import com.application.jokester.donation.dto.VerifyPaymentRequest;
import com.application.jokester.donation.entity.Donation;
import com.application.jokester.donation.repository.DonationRepository;
import com.application.jokester.donation.service.impl.DonationServiceImpl;
import com.application.jokester.event.DonationEvent;
import com.application.jokester.event.DonationEventProducer;
import com.application.jokester.exception.ExternalApiException;
import com.razorpay.Order;
import com.razorpay.OrderClient;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.razorpay.Utils;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class DonationServiceImplTest {

    @Mock
    private RazorpayClient razorpayClient;

    @Mock
    private OrderClient orderClient;

    @Mock
    private DonationRepository donationRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private DonationEventProducer donationEventProducer;

    @InjectMocks
    private DonationServiceImpl donationService;

    @Captor
    private ArgumentCaptor<Donation> donationCaptor;

    @BeforeEach
    void setUp() {
        razorpayClient.orders = orderClient;
        ReflectionTestUtils.setField(donationService, "razorpayKeyId", "rzp_test_key");
        ReflectionTestUtils.setField(donationService, "razorpayKeySecret", "rzp_test_secret");
    }

    @Test
    void should_CreateOrder_When_ValidRequest() throws RazorpayException {
        UUID userId = UUID.randomUUID();
        CreateDonationRequest request = new CreateDonationRequest();
        request.setAmount(BigDecimal.TEN);
        request.setCurrency("INR");

        User user = new User();
        user.setId(userId);
        user.setUsername("testuser");

        when(userRepository.getReferenceById(userId)).thenReturn(user);

        Order mockOrder = mock(Order.class);
        when(mockOrder.get("id")).thenReturn("order_123");
        when(orderClient.create(any(JSONObject.class))).thenReturn(mockOrder);

        RazorpayOrderResponse response = donationService.createOrder(request, userId);

        assertThat(response).isNotNull();
        assertThat(response.getOrderId()).isEqualTo("order_123");
        assertThat(response.getAmount()).isEqualTo(BigDecimal.TEN);
        assertThat(response.getCurrency()).isEqualTo("INR");
        assertThat(response.getKeyId()).isEqualTo("rzp_test_key");

        verify(donationRepository).save(donationCaptor.capture());
        Donation savedDonation = donationCaptor.getValue();
        assertThat(savedDonation.getUser()).isEqualTo(user);
        assertThat(savedDonation.getAmount()).isEqualTo(BigDecimal.TEN);
        assertThat(savedDonation.getRazorpayOrderId()).isEqualTo("order_123");
        assertThat(savedDonation.getStatus()).isEqualTo("CREATED");
    }

    @Test
    void should_ThrowExternalApiException_When_RazorpayFails() throws RazorpayException {
        UUID userId = UUID.randomUUID();
        CreateDonationRequest request = new CreateDonationRequest();
        request.setAmount(BigDecimal.TEN);
        request.setCurrency("INR");

        when(orderClient.create(any(JSONObject.class))).thenThrow(new RazorpayException("Razorpay error"));

        assertThatThrownBy(() -> donationService.createOrder(request, userId))
                .isInstanceOf(ExternalApiException.class)
                .hasMessageContaining("Payment service is temporarily unavailable");
    }

    @Test
    void should_VerifyPayment_When_ValidSignature() {
        UUID userId = UUID.randomUUID();
        VerifyPaymentRequest request = new VerifyPaymentRequest();
        request.setRazorpayOrderId("order_123");
        request.setRazorpayPaymentId("pay_123");
        request.setRazorpaySignature("valid_signature");

        User user = new User();
        user.setId(userId);
        user.setUsername("testuser");

        Donation donation = new Donation();
        donation.setId(UUID.randomUUID());
        donation.setUser(user);
        donation.setAmount(BigDecimal.TEN);
        donation.setCurrency("INR");
        donation.setRazorpayOrderId("order_123");
        donation.setStatus("CREATED");

        when(donationRepository.findByRazorpayOrderId("order_123")).thenReturn(Optional.of(donation));

        try (MockedStatic<Utils> mockedUtils = Mockito.mockStatic(Utils.class)) {
            mockedUtils.when(() -> Utils.verifyPaymentSignature(any(JSONObject.class), eq("rzp_test_secret")))
                    .thenReturn(true);

            DonationResponse response = donationService.verifyAndSavePayment(request, userId);

            assertThat(response).isNotNull();
            assertThat(response.getStatus()).isEqualTo("PAID");
            assertThat(response.getRazorpayPaymentId()).isEqualTo("pay_123");

            verify(donationRepository, times(1)).save(donation);
            verify(donationEventProducer).publishDonationEvent(any(DonationEvent.class));
            
            assertThat(donation.getStatus()).isEqualTo("PAID");
            assertThat(donation.getRazorpayPaymentId()).isEqualTo("pay_123");
        }
    }

    @Test
    void should_FailPayment_When_InvalidSignature() {
        UUID userId = UUID.randomUUID();
        VerifyPaymentRequest request = new VerifyPaymentRequest();
        request.setRazorpayOrderId("order_123");
        request.setRazorpayPaymentId("pay_123");
        request.setRazorpaySignature("invalid_signature");

        User user = new User();
        user.setId(userId);
        user.setUsername("testuser");

        Donation donation = new Donation();
        donation.setId(UUID.randomUUID());
        donation.setUser(user);
        donation.setAmount(BigDecimal.TEN);
        donation.setCurrency("INR");
        donation.setRazorpayOrderId("order_123");
        donation.setStatus("CREATED");

        when(donationRepository.findByRazorpayOrderId("order_123")).thenReturn(Optional.of(donation));

        try (MockedStatic<Utils> mockedUtils = Mockito.mockStatic(Utils.class)) {
            mockedUtils.when(() -> Utils.verifyPaymentSignature(any(JSONObject.class), eq("rzp_test_secret")))
                    .thenReturn(false);

            assertThatThrownBy(() -> donationService.verifyAndSavePayment(request, userId))
                    .isInstanceOf(ExternalApiException.class)
                    .hasMessageContaining("Payment verification failed. Invalid signature.");

            verify(donationRepository).save(donation);
            assertThat(donation.getStatus()).isEqualTo("FAILED");
            verify(donationEventProducer, never()).publishDonationEvent(any());
        }
    }

    @Test
    void should_GetMyDonations_When_UserHasDonations() {
        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setId(userId);
        user.setUsername("testuser");

        Donation donation = new Donation();
        donation.setId(UUID.randomUUID());
        donation.setUser(user);
        donation.setAmount(BigDecimal.TEN);
        donation.setCurrency("INR");
        donation.setRazorpayOrderId("order_123");
        donation.setStatus("PAID");

        Page<Donation> page = new PageImpl<>(List.of(donation));
        when(donationRepository.findByUserIdOrderByCreatedAtDesc(eq(userId), any(PageRequest.class)))
                .thenReturn(page);

        Page<DonationResponse> response = donationService.getMyDonations(userId, 0, 10);

        assertThat(response).isNotNull();
        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getContent().get(0).getDonorUsername()).isEqualTo("testuser");
    }
}
