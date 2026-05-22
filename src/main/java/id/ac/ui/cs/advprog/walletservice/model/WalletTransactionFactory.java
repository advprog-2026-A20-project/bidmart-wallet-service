package id.ac.ui.cs.advprog.walletservice.model;

import java.math.BigDecimal;

public final class WalletTransactionFactory {
    private WalletTransactionFactory() {
    }

    public static WalletTransaction createFrom(Wallet wallet, String type, BigDecimal amount, String reference) {
        return new WalletTransaction(
            wallet.getUserId(),
            type,
            amount,
            reference,
            wallet.getAvailableBalance(),
            wallet.getHeldBalance()
        );
    }
}
