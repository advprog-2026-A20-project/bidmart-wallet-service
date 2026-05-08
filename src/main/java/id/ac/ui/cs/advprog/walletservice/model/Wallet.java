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
        this.availableBalance = this.availableBalance.add(amount);
    }

    public void withdraw(BigDecimal amount) {
        this.availableBalance = this.availableBalance.subtract(amount);
    }

    public void hold(BigDecimal amount) {
        this.availableBalance = this.availableBalance.subtract(amount);
        this.heldBalance = this.heldBalance.add(amount);
    }

    public void release(BigDecimal amount) {
        this.heldBalance = this.heldBalance.subtract(amount);
        this.availableBalance = this.availableBalance.add(amount);
    }

    public void capture(BigDecimal amount) {
        this.heldBalance = this.heldBalance.subtract(amount);
    }
}
