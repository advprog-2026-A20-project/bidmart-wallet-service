package id.ac.ui.cs.advprog.walletservice.model;

import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WalletTransactionFactoryTest {
    private static final String AMOUNT_30000 = "30000";
    private static final String AMOUNT_70000 = "70000";
    private static final String AMOUNT_100000 = "100000";
    private static final String TEST_REFERENCE = "test-reference";

    @Test
    void createFromCopiesWalletTransactionValuesAndBalanceSnapshot() {
        Wallet wallet = new Wallet(UUID.randomUUID());
        wallet.topUp(new BigDecimal(AMOUNT_100000));
        wallet.hold(new BigDecimal(AMOUNT_30000));

        WalletTransaction transaction = WalletTransactionFactory.createFrom(
            wallet,
            WalletTransactionConstants.HOLD,
            new BigDecimal(AMOUNT_30000),
            TEST_REFERENCE
        );

        assertEquals(wallet.getUserId(), transaction.getUserId());
        assertEquals(WalletTransactionConstants.HOLD, transaction.getType());
        assertMoney(AMOUNT_30000, transaction.getAmount());
        assertEquals(TEST_REFERENCE, transaction.getReference());
        assertMoney(AMOUNT_70000, transaction.getAvailableBalanceAfter());
        assertMoney(AMOUNT_30000, transaction.getHeldBalanceAfter());
    }

    private void assertMoney(String expected, BigDecimal actual) {
        assertEquals(0, actual.compareTo(new BigDecimal(expected)));
    }
}
