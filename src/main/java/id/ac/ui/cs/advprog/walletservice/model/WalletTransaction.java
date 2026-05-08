package id.ac.ui.cs.advprog.walletservice.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class WalletTransaction {
    private final UUID transactionId;
    private final UUID userId;
    private final String type;
    private final BigDecimal amount;
    private final String reference;
    private final Instant timestamp;

    public WalletTransaction(UUID userId, String type, BigDecimal amount, String reference) {
        this.transactionId = UUID.randomUUID();
        this.userId = userId;
        this.type = type;
        this.amount = amount;
        this.reference = reference;
        this.timestamp = Instant.now();
    }

    public UUID getTransactionId() { return transactionId; }
    public UUID getUserId() { return userId; }
    public String getType() { return type; }
    public BigDecimal getAmount() { return amount; }
    public String getReference() { return reference; }
    public Instant getTimestamp() { return timestamp; }
}
