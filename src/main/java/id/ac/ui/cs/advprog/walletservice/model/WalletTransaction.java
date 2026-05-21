package id.ac.ui.cs.advprog.walletservice.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "wallet_transaction")
public class WalletTransaction {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID transactionId;

    @Column(nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private String type;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column
    private String reference;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal availableBalanceAfter;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal heldBalanceAfter;

    @Column(nullable = false, updatable = false)
    private Instant timestamp;

    protected WalletTransaction() {
    }

    public WalletTransaction(
        UUID userId,
        String type,
        BigDecimal amount,
        String reference,
        BigDecimal availableBalanceAfter,
        BigDecimal heldBalanceAfter
    ) {
        this.transactionId = UUID.randomUUID();
        this.userId = userId;
        this.type = type;
        this.amount = amount;
        this.reference = reference;
        this.availableBalanceAfter = availableBalanceAfter;
        this.heldBalanceAfter = heldBalanceAfter;
        this.timestamp = Instant.now();
    }

    public UUID getTransactionId() { return transactionId; }
    public UUID getUserId() { return userId; }
    public String getType() { return type; }
    public BigDecimal getAmount() { return amount; }
    public String getReference() { return reference; }
    public BigDecimal getAvailableBalanceAfter() { return availableBalanceAfter; }
    public BigDecimal getHeldBalanceAfter() { return heldBalanceAfter; }
    public Instant getTimestamp() { return timestamp; }
}
