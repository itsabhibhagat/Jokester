package com.application.jokester.donation.repository;

import com.application.jokester.donation.entity.Donation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface DonationRepository extends JpaRepository<Donation, UUID> {

    Optional<Donation> findByRazorpayOrderId(String razorpayOrderId);

    Page<Donation> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);
}
