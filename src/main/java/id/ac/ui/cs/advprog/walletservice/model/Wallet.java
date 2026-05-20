package id.ac.ui.cs.advprog.walletservice.model;

import java.math.BigDecimal;
import java.util.UUID;

public class Wallet {
    private final UUID userId;
    private BigDecimal availableBalance;
    private BigDecimal heldBalance;

    public Wallet(UUID userId) {
        this.userId = userId;
        this.availableBalance = BigDecimal.ZERO;
        this.heldBalance = BigDecimal.ZERO;
    }

    public UUID getUserId() {
        return userId;
    }

    public BigDecimal getAvailableBalance() {
        return availableBalance;
    }

    public BigDecimal getHeldBalance() {
        return heldBalance;
    }

    public void topUp(BigDecimal amount) {
        validatePositiveAmount(amount);
        this.availableBalance = this.availableBalance.add(amount);
    }

    public void withdraw(BigDecimal amount) {
        validatePositiveAmount(amount);
        ensureAvailableBalance(amount);
        this.availableBalance = this.availableBalance.subtract(amount);
    }

    public void hold(BigDecimal amount) {
        validatePositiveAmount(amount);
        ensureAvailableBalance(amount);
        this.availableBalance = this.availableBalance.subtract(amount);
        this.heldBalance = this.heldBalance.add(amount);
    }

    public void release(BigDecimal amount) {
        validatePositiveAmount(amount);
        ensureHeldBalance(amount);
        this.heldBalance = this.heldBalance.subtract(amount);
        this.availableBalance = this.availableBalance.add(amount);
    }

    public void capture(BigDecimal amount) {
        validatePositiveAmount(amount);
        ensureHeldBalance(amount);
        this.heldBalance = this.heldBalance.subtract(amount);
    }

    private void validatePositiveAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero");
        }
    }

    private void ensureAvailableBalance(BigDecimal amount) {
        if (this.availableBalance.compareTo(amount) < 0) {
            throw new IllegalArgumentException("Insufficient balance");
        }
    }

    private void ensureHeldBalance(BigDecimal amount) {
        if (this.heldBalance.compareTo(amount) < 0) {
            throw new IllegalArgumentException("Insufficient held balance");
        }
    }
}
