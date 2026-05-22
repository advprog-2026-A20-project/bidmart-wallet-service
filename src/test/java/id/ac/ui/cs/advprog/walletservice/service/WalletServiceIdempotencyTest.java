package id.ac.ui.cs.advprog.walletservice.service;

import id.ac.ui.cs.advprog.walletservice.model.HoldRecord;
import id.ac.ui.cs.advprog.walletservice.model.WalletTransactionConstants;
import id.ac.ui.cs.advprog.walletservice.repository.HoldRepository;
import id.ac.ui.cs.advprog.walletservice.repository.TransactionRepository;
import id.ac.ui.cs.advprog.walletservice.repository.WalletRepository;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static id.ac.ui.cs.advprog.walletservice.service.WalletServiceTestAssertions.assertBalance;
import static id.ac.ui.cs.advprog.walletservice.service.WalletServiceTestAssertions.assertSingleTransaction;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
class WalletServiceIdempotencyTest {
    private static final String AMOUNT_0 = "0";
    private static final String AMOUNT_30000 = "30000";
    private static final String AMOUNT_70000 = "70000";
    private static final String AMOUNT_100000 = "100000";

    @Autowired
    private WalletService walletService;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private HoldRepository holdRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @BeforeEach
    void setUp() {
        transactionRepository.deleteAll();
        holdRepository.deleteAll();
        walletRepository.deleteAll();
    }

    @Test
    void holdReturnsCachedResultAndRecordsTransactionOnceForSameIdempotencyKey() {
        UUID userId = UUID.randomUUID();
        String idempotencyKey = "hold-key-" + UUID.randomUUID();
        walletService.topUp(userId, new BigDecimal(AMOUNT_100000));
        transactionRepository.deleteAll();

        HoldRecord firstHold = walletService.hold(userId, new BigDecimal(AMOUNT_30000), idempotencyKey);
        HoldRecord secondHold = walletService.hold(userId, new BigDecimal(AMOUNT_30000), idempotencyKey);

        assertEquals(firstHold.getHoldId(), secondHold.getHoldId());
        assertEquals(HoldRecord.HoldStatus.HELD, firstHold.getStatus());

        assertBalance(walletService, userId, AMOUNT_70000, AMOUNT_30000);
        assertSingleTransaction(
                walletService,
                userId,
                WalletTransactionConstants.HOLD,
                AMOUNT_30000,
                firstHold.getHoldId().toString(),
                AMOUNT_70000,
                AMOUNT_30000
        );
    }

    @Test
    void releaseReturnsCachedResultAndRecordsTransactionOnceForSameIdempotencyKey() {
        UUID userId = UUID.randomUUID();
        String holdIdempotencyKey = "hold-setup-key-" + UUID.randomUUID();
        String releaseIdempotencyKey = "release-key-" + UUID.randomUUID();
        walletService.topUp(userId, new BigDecimal(AMOUNT_100000));
        HoldRecord holdRecord = walletService.hold(userId, new BigDecimal(AMOUNT_30000), holdIdempotencyKey);
        transactionRepository.deleteAll();

        HoldRecord firstRelease = walletService.release(userId, holdRecord.getHoldId(), releaseIdempotencyKey);
        HoldRecord secondRelease = walletService.release(userId, holdRecord.getHoldId(), releaseIdempotencyKey);

        assertEquals(firstRelease.getHoldId(), secondRelease.getHoldId());
        assertEquals(HoldRecord.HoldStatus.RELEASED, firstRelease.getStatus());

        assertBalance(walletService, userId, AMOUNT_100000, AMOUNT_0);
        assertSingleTransaction(
                walletService,
                userId,
                WalletTransactionConstants.RELEASE,
                AMOUNT_30000,
                holdRecord.getHoldId().toString(),
                AMOUNT_100000,
                AMOUNT_0
        );
    }

    @Test
    void captureReturnsCachedResultAndRecordsTransactionOnceForSameIdempotencyKey() {
        UUID userId = UUID.randomUUID();
        String holdIdempotencyKey = "hold-setup-key-" + UUID.randomUUID();
        String captureIdempotencyKey = "capture-key-" + UUID.randomUUID();
        walletService.topUp(userId, new BigDecimal(AMOUNT_100000));
        HoldRecord holdRecord = walletService.hold(userId, new BigDecimal(AMOUNT_30000), holdIdempotencyKey);
        transactionRepository.deleteAll();

        HoldRecord firstCapture = walletService.capture(userId, holdRecord.getHoldId(), captureIdempotencyKey);
        HoldRecord secondCapture = walletService.capture(userId, holdRecord.getHoldId(), captureIdempotencyKey);

        assertEquals(firstCapture.getHoldId(), secondCapture.getHoldId());
        assertEquals(HoldRecord.HoldStatus.CAPTURED, firstCapture.getStatus());

        assertBalance(walletService, userId, AMOUNT_70000, AMOUNT_0);
        assertSingleTransaction(
                walletService,
                userId,
                WalletTransactionConstants.CAPTURE,
                AMOUNT_30000,
                holdRecord.getHoldId().toString(),
                AMOUNT_70000,
                AMOUNT_0
        );
    }
}
