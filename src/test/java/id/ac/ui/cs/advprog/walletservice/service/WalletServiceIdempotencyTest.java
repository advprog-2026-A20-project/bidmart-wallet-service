package id.ac.ui.cs.advprog.walletservice.service;

import id.ac.ui.cs.advprog.walletservice.dto.WalletBalanceResponse;
import id.ac.ui.cs.advprog.walletservice.model.HoldRecord;
import id.ac.ui.cs.advprog.walletservice.model.WalletTransaction;
import id.ac.ui.cs.advprog.walletservice.model.WalletTransactionConstants;
import id.ac.ui.cs.advprog.walletservice.repository.HoldRepository;
import id.ac.ui.cs.advprog.walletservice.repository.TransactionRepository;
import id.ac.ui.cs.advprog.walletservice.repository.WalletRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

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

        WalletBalanceResponse balance = walletService.getBalance(userId);
        assertMoney(AMOUNT_70000, balance.availableBalance());
        assertMoney(AMOUNT_30000, balance.heldBalance());

        List<WalletTransaction> transactions = walletService.getTransactions(userId);
        assertEquals(1, transactions.size());

        WalletTransaction transaction = transactions.get(0);
        assertEquals(userId, transaction.getUserId());
        assertEquals(WalletTransactionConstants.HOLD, transaction.getType());
        assertMoney(AMOUNT_30000, transaction.getAmount());
        assertEquals(firstHold.getHoldId().toString(), transaction.getReference());
        assertMoney(AMOUNT_70000, transaction.getAvailableBalanceAfter());
        assertMoney(AMOUNT_30000, transaction.getHeldBalanceAfter());
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

        WalletBalanceResponse balance = walletService.getBalance(userId);
        assertMoney(AMOUNT_100000, balance.availableBalance());
        assertMoney(AMOUNT_0, balance.heldBalance());

        List<WalletTransaction> transactions = walletService.getTransactions(userId);
        assertEquals(1, transactions.size());

        WalletTransaction transaction = transactions.get(0);
        assertEquals(userId, transaction.getUserId());
        assertEquals(WalletTransactionConstants.RELEASE, transaction.getType());
        assertMoney(AMOUNT_30000, transaction.getAmount());
        assertEquals(holdRecord.getHoldId().toString(), transaction.getReference());
        assertMoney(AMOUNT_100000, transaction.getAvailableBalanceAfter());
        assertMoney(AMOUNT_0, transaction.getHeldBalanceAfter());
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

        WalletBalanceResponse balance = walletService.getBalance(userId);
        assertMoney(AMOUNT_70000, balance.availableBalance());
        assertMoney(AMOUNT_0, balance.heldBalance());

        List<WalletTransaction> transactions = walletService.getTransactions(userId);
        assertEquals(1, transactions.size());

        WalletTransaction transaction = transactions.get(0);
        assertEquals(userId, transaction.getUserId());
        assertEquals(WalletTransactionConstants.CAPTURE, transaction.getType());
        assertMoney(AMOUNT_30000, transaction.getAmount());
        assertEquals(holdRecord.getHoldId().toString(), transaction.getReference());
        assertMoney(AMOUNT_70000, transaction.getAvailableBalanceAfter());
        assertMoney(AMOUNT_0, transaction.getHeldBalanceAfter());
    }

    private void assertMoney(String expected, BigDecimal actual) {
        assertEquals(0, actual.compareTo(new BigDecimal(expected)));
    }
}
