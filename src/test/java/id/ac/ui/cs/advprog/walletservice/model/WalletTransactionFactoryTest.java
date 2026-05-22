package id.ac.ui.cs.advprog.walletservice.model;

import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WalletTransactionFactoryTest {

    @Test
    void createFromCopiesWalletTransactionValuesAndBalanceSnapshot() {
        Wallet wallet = new Wallet(UUID.randomUUID());
        wallet.topUp(new BigDecimal("100000"));
        wallet.hold(new BigDecimal("30000"));

        WalletTransaction transaction = WalletTransactionFactory.createFrom(
            wallet,
            WalletTransactionConstants.HOLD,
            new BigDecimal("30000"),
            "test-reference"
        );

        assertEquals(wallet.getUserId(), transaction.getUserId());
        assertEquals(WalletTransactionConstants.HOLD, transaction.getType());
        assertMoney("30000", transaction.getAmount());
        assertEquals("test-reference", transaction.getReference());
        assertMoney("70000", transaction.getAvailableBalanceAfter());
        assertMoney("30000", transaction.getHeldBalanceAfter());
    }

    private void assertMoney(String expected, BigDecimal actual) {
        assertEquals(0, actual.compareTo(new BigDecimal(expected)));
    }
}
