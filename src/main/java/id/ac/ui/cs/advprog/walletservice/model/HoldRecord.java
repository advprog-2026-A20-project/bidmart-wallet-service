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
    private final BigDecimal amount;
    private HoldStatus status;
    private final Instant createdAt;

    public HoldRecord(UUID holdId, UUID userId, BigDecimal amount) {
        this.holdId = holdId;
        this.userId = userId;
        this.amount = amount;
        this.status = HoldStatus.HELD;
        this.createdAt = Instant.now();
    }

    public UUID getHoldId() { return holdId; }
    public UUID getUserId() { return userId; }
    public BigDecimal getAmount() { return amount; }
    public HoldStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }

    public void markReleased() { this.status = HoldStatus.RELEASED; }
    public void markCaptured() { this.status = HoldStatus.CAPTURED; }
}
