package xyz.imaketech.holdmyseat.payment.domain;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** booking_id is unique: the idempotency key. */
@Entity
@Table(name = "payments")
public class Payment {

    @Id
    private UUID id = UUID.randomUUID();

    @Column(name = "booking_id", nullable = false, unique = true)
    private UUID bookingId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;

    @Column(name = "failure_reason")
    private String failureReason;

    @Column(name = "provider_ref")
    private String providerRef;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @Version
    private Long version;

    protected Payment() {
    }

    public Payment(UUID bookingId, UUID userId, BigDecimal amount, SimulatedPaymentProvider.Result result) {
        this.bookingId = bookingId;
        this.userId = userId;
        this.amount = amount;
        this.status = result.success() ? PaymentStatus.SUCCEEDED : PaymentStatus.FAILED;
        this.failureReason = result.failureReason();
        this.providerRef = result.providerRef();
    }

    public void refund() {
        this.status = PaymentStatus.REFUNDED;
        this.updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getBookingId() { return bookingId; }
    public UUID getUserId() { return userId; }
    public BigDecimal getAmount() { return amount; }
    public PaymentStatus getStatus() { return status; }
    public String getFailureReason() { return failureReason; }
    public Instant getCreatedAt() { return createdAt; }
}
