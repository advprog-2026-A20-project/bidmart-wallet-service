package id.ac.ui.cs.advprog.walletservice.service;

import id.ac.ui.cs.advprog.walletservice.dto.WalletBalanceResponse;
import id.ac.ui.cs.advprog.walletservice.model.WalletTransaction;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class WalletServiceTestAssertions {
    private WalletServiceTestAssertions() {
    }

    static void assertBalance(
            WalletService walletService,
            UUID userId,
            String expectedAvailable,
            String expectedHeld
    ) {
        WalletBalanceResponse balance = walletService.getBalance(userId);
        assertMoney(expectedAvailable, balance.availableBalance());
        assertMoney(expectedHeld, balance.heldBalance());
    }

    static void assertSingleTransaction(
            WalletService walletService,
            UUID userId,
            String expectedType,
            String expectedAmount,
            String expectedReference,
            String expectedAvailableAfter,
            String expectedHeldAfter
    ) {
        List<WalletTransaction> transactions = walletService.getTransactions(userId);
        assertEquals(1, transactions.size());

        WalletTransaction transaction = transactions.get(0);
        assertEquals(userId, transaction.getUserId());
        assertEquals(expectedType, transaction.getType());
        assertMoney(expectedAmount, transaction.getAmount());
        assertEquals(expectedReference, transaction.getReference());
        assertMoney(expectedAvailableAfter, transaction.getAvailableBalanceAfter());
        assertMoney(expectedHeldAfter, transaction.getHeldBalanceAfter());
    }

    static void assertMoney(String expected, BigDecimal actual) {
        assertEquals(0, actual.compareTo(new BigDecimal(expected)));
    }
}
