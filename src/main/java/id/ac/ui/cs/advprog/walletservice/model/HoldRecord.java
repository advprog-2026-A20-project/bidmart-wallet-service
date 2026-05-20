package id.ac.ui.cs.advprog.walletservice.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class HoldRecord {
    public enum HoldStatus {
        HELD,
        RELEASED,
        CAPTURED
    }

    private final UUID holdId;
    private final UUID userId;
    private final UUID auctionId;
    private BigDecimal amount;
    private HoldStatus status;
    private final Instant createdAt;

    public HoldRecord(UUID holdId, UUID userId, BigDecimal amount) {
        this(holdId, userId, null, amount);
    }

    public HoldRecord(UUID holdId, UUID userId, UUID auctionId, BigDecimal amount) {
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

    public void increaseAmount(BigDecimal additionalAmount) { this.amount = this.amount.add(additionalAmount); }
    public void markReleased() { this.status = HoldStatus.RELEASED; }
    public void markCaptured() { this.status = HoldStatus.CAPTURED; }
}
