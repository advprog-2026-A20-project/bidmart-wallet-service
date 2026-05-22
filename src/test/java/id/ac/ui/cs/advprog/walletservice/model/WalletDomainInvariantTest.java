package id.ac.ui.cs.advprog.walletservice.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class WalletDomainInvariantTest {
    private static final String AMOUNT_NEGATIVE_1 = "-1";
    private static final String AMOUNT_10000 = "10000";
    private static final String AMOUNT_10001 = "10001";
    private static final String AMOUNT_50000 = "50000";
    private static final String AMOUNT_50001 = "50001";
    private static final String AMOUNT_100000 = "100000";

    @Test
    void walletRejectsNegativeOrZeroTopUpAmounts() {
        Wallet wallet = new Wallet(UUID.randomUUID());

        assertInvalidAmount(() -> wallet.topUp(BigDecimal.ZERO));
        assertInvalidAmount(() -> wallet.topUp(new BigDecimal(AMOUNT_NEGATIVE_1)));
        assertInvalidAmount(() -> wallet.topUp(null));
        assertEquals(BigDecimal.ZERO, wallet.getAvailableBalance());
    }

    @Test
    void walletRejectsNegativeOrZeroWithdrawAmounts() {
        Wallet wallet = new Wallet(UUID.randomUUID());
        wallet.topUp(new BigDecimal(AMOUNT_100000));

        assertInvalidAmount(() -> wallet.withdraw(BigDecimal.ZERO));
        assertInvalidAmount(() -> wallet.withdraw(new BigDecimal(AMOUNT_NEGATIVE_1)));
        assertInvalidAmount(() -> wallet.withdraw(null));
        assertEquals(new BigDecimal(AMOUNT_100000), wallet.getAvailableBalance());
    }

    @Test
    void walletRejectsNegativeOrZeroHoldAmounts() {
        Wallet wallet = new Wallet(UUID.randomUUID());
        wallet.topUp(new BigDecimal(AMOUNT_100000));

        assertInvalidAmount(() -> wallet.hold(BigDecimal.ZERO));
        assertInvalidAmount(() -> wallet.hold(new BigDecimal(AMOUNT_NEGATIVE_1)));
        assertInvalidAmount(() -> wallet.hold(null));
        assertEquals(new BigDecimal(AMOUNT_100000), wallet.getAvailableBalance());
        assertEquals(BigDecimal.ZERO, wallet.getHeldBalance());
    }

    @Test
    void walletRejectsNegativeOrZeroReleaseAmounts() {
        Wallet wallet = walletWithHeldBalance(new BigDecimal(AMOUNT_50000));

        assertInvalidAmount(() -> wallet.release(BigDecimal.ZERO));
        assertInvalidAmount(() -> wallet.release(new BigDecimal(AMOUNT_NEGATIVE_1)));
        assertInvalidAmount(() -> wallet.release(null));
        assertEquals(new BigDecimal(AMOUNT_50000), wallet.getHeldBalance());
    }

    @Test
    void walletRejectsNegativeOrZeroCaptureAmounts() {
        Wallet wallet = walletWithHeldBalance(new BigDecimal(AMOUNT_50000));

        assertInvalidAmount(() -> wallet.capture(BigDecimal.ZERO));
        assertInvalidAmount(() -> wallet.capture(new BigDecimal(AMOUNT_NEGATIVE_1)));
        assertInvalidAmount(() -> wallet.capture(null));
        assertEquals(new BigDecimal(AMOUNT_50000), wallet.getHeldBalance());
    }

    @Test
    void walletHoldRejectsAmountLargerThanAvailableBalance() {
        Wallet wallet = new Wallet(UUID.randomUUID());
        wallet.topUp(new BigDecimal(AMOUNT_10000));

        assertThrows(IllegalArgumentException.class, () -> wallet.hold(new BigDecimal(AMOUNT_10001)));
        assertEquals(new BigDecimal(AMOUNT_10000), wallet.getAvailableBalance());
        assertEquals(BigDecimal.ZERO, wallet.getHeldBalance());
    }

    @Test
    void walletWithdrawRejectsAmountLargerThanAvailableBalance() {
        Wallet wallet = new Wallet(UUID.randomUUID());
        wallet.topUp(new BigDecimal(AMOUNT_10000));

        assertThrows(IllegalArgumentException.class, () -> wallet.withdraw(new BigDecimal(AMOUNT_10001)));
        assertEquals(new BigDecimal(AMOUNT_10000), wallet.getAvailableBalance());
        assertEquals(BigDecimal.ZERO, wallet.getHeldBalance());
    }

    @Test
    void walletReleaseRejectsAmountLargerThanHeldBalance() {
        Wallet wallet = walletWithHeldBalance(new BigDecimal(AMOUNT_50000));

        assertThrows(IllegalArgumentException.class, () -> wallet.release(new BigDecimal(AMOUNT_50001)));
        assertEquals(BigDecimal.ZERO, wallet.getAvailableBalance());
        assertEquals(new BigDecimal(AMOUNT_50000), wallet.getHeldBalance());
    }

    @Test
    void walletCaptureRejectsAmountLargerThanHeldBalance() {
        Wallet wallet = walletWithHeldBalance(new BigDecimal(AMOUNT_50000));

        assertThrows(IllegalArgumentException.class, () -> wallet.capture(new BigDecimal(AMOUNT_50001)));
        assertEquals(BigDecimal.ZERO, wallet.getAvailableBalance());
        assertEquals(new BigDecimal(AMOUNT_50000), wallet.getHeldBalance());
    }

    @Test
    void holdRecordCannotBeReleasedTwice() {
        HoldRecord holdRecord = new HoldRecord(UUID.randomUUID(), UUID.randomUUID(), new BigDecimal(AMOUNT_50000));
        holdRecord.markReleased();

        assertThrows(IllegalStateException.class, holdRecord::markReleased);
        assertEquals(HoldRecord.HoldStatus.RELEASED, holdRecord.getStatus());
    }

    @Test
    void holdRecordCannotBeCapturedAfterRelease() {
        HoldRecord holdRecord = new HoldRecord(UUID.randomUUID(), UUID.randomUUID(), new BigDecimal(AMOUNT_50000));
        holdRecord.markReleased();

        assertThrows(IllegalStateException.class, holdRecord::markCaptured);
        assertEquals(HoldRecord.HoldStatus.RELEASED, holdRecord.getStatus());
    }

    @Test
    void holdRecordCannotBeReleasedAfterCapture() {
        HoldRecord holdRecord = new HoldRecord(UUID.randomUUID(), UUID.randomUUID(), new BigDecimal(AMOUNT_50000));
        holdRecord.markCaptured();

        assertThrows(IllegalStateException.class, holdRecord::markReleased);
        assertEquals(HoldRecord.HoldStatus.CAPTURED, holdRecord.getStatus());
    }

    @Test
    void holdRecordCannotIncreaseAmountAfterReleaseOrCapture() {
        HoldRecord releasedHold = new HoldRecord(UUID.randomUUID(), UUID.randomUUID(), new BigDecimal(AMOUNT_50000));
        HoldRecord capturedHold = new HoldRecord(UUID.randomUUID(), UUID.randomUUID(), new BigDecimal(AMOUNT_50000));
        releasedHold.markReleased();
        capturedHold.markCaptured();

        assertThrows(IllegalStateException.class, () -> releasedHold.increaseAmount(new BigDecimal(AMOUNT_10000)));
        assertThrows(IllegalStateException.class, () -> capturedHold.increaseAmount(new BigDecimal(AMOUNT_10000)));
        assertEquals(new BigDecimal(AMOUNT_50000), releasedHold.getAmount());
        assertEquals(new BigDecimal(AMOUNT_50000), capturedHold.getAmount());
    }

    @Test
    void holdRecordRejectsNegativeOrZeroIncreaseAmounts() {
        HoldRecord holdRecord = new HoldRecord(UUID.randomUUID(), UUID.randomUUID(), new BigDecimal(AMOUNT_50000));

        assertInvalidAmount(() -> holdRecord.increaseAmount(BigDecimal.ZERO));
        assertInvalidAmount(() -> holdRecord.increaseAmount(new BigDecimal(AMOUNT_NEGATIVE_1)));
        assertInvalidAmount(() -> holdRecord.increaseAmount(null));
        assertEquals(new BigDecimal(AMOUNT_50000), holdRecord.getAmount());
    }

    private Wallet walletWithHeldBalance(BigDecimal amount) {
        Wallet wallet = new Wallet(UUID.randomUUID());
        wallet.topUp(amount);
        wallet.hold(amount);
        return wallet;
    }

    private void assertInvalidAmount(Runnable action) {
        assertThrows(IllegalArgumentException.class, action::run);
    }
}
