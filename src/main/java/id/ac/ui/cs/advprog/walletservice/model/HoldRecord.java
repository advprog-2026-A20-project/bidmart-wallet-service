package id.ac.ui.cs.advprog.walletservice.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "wallet_hold")
public class HoldRecord {
    public enum HoldStatus {
        HELD,
        RELEASED,
        CAPTURED
    }

    @Id
    @Column(nullable = false, updatable = false)
    private UUID holdId;

    @Column(nullable = false)
    private UUID userId;

    @Column
    private UUID auctionId;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private HoldStatus status;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Version
    private Long version;

    protected HoldRecord() {
    }

    public HoldRecord(UUID holdId, UUID userId, BigDecimal amount) {
        this(holdId, userId, null, amount);
    }

    public HoldRecord(UUID holdId, UUID userId, UUID auctionId, BigDecimal amount) {
        validatePositiveAmount(amount);
        this.holdId = holdId;
        this.userId = userId;
        this.auctionId = auctionId;
        this.amount = amount;
        this.status = HoldStatus.HELD;
        this.createdAt = Instant.now();
    }

    public UUID getHoldId() { return holdId; }
    public UUID getUserId() { return userId; }
    public UUID getAuctionId() { return auctionId; }
    public BigDecimal getAmount() { return amount; }
    public HoldStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Long getVersion() { return version; }

    public void increaseAmount(BigDecimal additionalAmount) {
        validateHeldStatus();
        validatePositiveAmount(additionalAmount);
        this.amount = this.amount.add(additionalAmount);
    }

    public void markReleased() {
        validateHeldStatus();
        this.status = HoldStatus.RELEASED;
    }

    public void markCaptured() {
        validateHeldStatus();
        this.status = HoldStatus.CAPTURED;
    }

    private void validatePositiveAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero");
        }
    }

    private void validateHeldStatus() {
        if (this.status != HoldStatus.HELD) {
            throw new IllegalStateException("Hold must be HELD");
        }
    }
}
