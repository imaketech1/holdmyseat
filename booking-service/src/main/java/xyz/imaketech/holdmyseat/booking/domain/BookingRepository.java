package xyz.imaketech.holdmyseat.booking.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BookingRepository extends JpaRepository<Booking, UUID> {

    Optional<Booking> findByIdAndUserId(UUID id, UUID userId);

    List<Booking> findTop50ByUserIdOrderByCreatedAtDesc(UUID userId);

    /** SKIP LOCKED lets replicas sweep in parallel. */
    @Query(value = """
            select * from bookings
            where status in ('HELD', 'PAYMENT_PENDING') and expires_at < now()
            order by expires_at
            limit :limit
            for update skip locked""", nativeQuery = true)
    List<Booking> lockOverdue(int limit);
}
