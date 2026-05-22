package id.ac.ui.cs.advprog.walletservice.service;

import id.ac.ui.cs.advprog.walletservice.model.HoldRecord;
import id.ac.ui.cs.advprog.walletservice.model.WalletTransaction;
import id.ac.ui.cs.advprog.walletservice.model.WalletTransactionConstants;
import id.ac.ui.cs.advprog.walletservice.repository.HoldRepository;
import id.ac.ui.cs.advprog.walletservice.repository.TransactionRepository;
import id.ac.ui.cs.advprog.walletservice.repository.WalletRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static id.ac.ui.cs.advprog.walletservice.service.WalletServiceTestAssertions.assertBalance;
import static id.ac.ui.cs.advprog.walletservice.service.WalletServiceTestAssertions.assertMoney;
import static id.ac.ui.cs.advprog.walletservice.service.WalletServiceTestAssertions.assertSingleTransaction;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
class WalletServiceInternalAuctionFlowTest {
    private static final String AMOUNT_0 = "0";
    private static final String AMOUNT_1000 = "1000";
    private static final String AMOUNT_2000 = "2000";
    private static final String AMOUNT_10000 = "10000";
    private static final String AMOUNT_15000 = "15000";
    private static final String AMOUNT_25000 = "25000";
    private static final String AMOUNT_30000 = "30000";
    private static final String AMOUNT_40000 = "40000";
    private static final String AMOUNT_60000 = "60000";
    private static final String AMOUNT_70000 = "70000";
    private static final String AMOUNT_75000 = "75000";
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
    void holdForAuctionMovesAmountFromAvailableBalanceToHeldBalance() {
        UUID userId = randomUserId();
        UUID auctionId = randomAuctionId();
        topUp(userId, AMOUNT_100000);

        HoldRecord holdRecord = holdForAuction(userId, auctionId, AMOUNT_25000);

        assertBalance(walletService, userId, AMOUNT_75000, AMOUNT_25000);
        assertHoldStatus(holdRecord, AMOUNT_25000, HoldRecord.HoldStatus.HELD);
    }

    @Test
    void holdForAuctionRecordsAuctionReferenceAndEndingBalances() {
        UUID userId = randomUserId();
        UUID auctionId = randomAuctionId();
        topUp(userId, AMOUNT_100000);
        transactionRepository.deleteAll();

        holdForAuction(userId, auctionId, AMOUNT_30000);

        assertBalance(walletService, userId, AMOUNT_70000, AMOUNT_30000);
        assertSingleTransaction(
                walletService,
                userId,
                WalletTransactionConstants.HOLD,
                AMOUNT_30000,
                auctionId.toString(),
                AMOUNT_70000,
                AMOUNT_30000
        );
    }

    @Test
    void holdForAuctionIncreasesExistingActiveHoldForSameUserAndAuction() {
        UUID userId = randomUserId();
        UUID auctionId = randomAuctionId();
        topUp(userId, AMOUNT_100000);
        HoldRecord firstHold = holdForAuction(userId, auctionId, AMOUNT_25000);

        HoldRecord secondHold = holdForAuction(userId, auctionId, AMOUNT_15000);

        assertEquals(firstHold.getHoldId(), secondHold.getHoldId());
        assertBalance(walletService, userId, AMOUNT_60000, AMOUNT_40000);
        assertHoldStatus(secondHold, AMOUNT_40000, HoldRecord.HoldStatus.HELD);

        List<WalletTransaction> transactions = walletService.getTransactions(userId);
        assertEquals(3, transactions.size());
        assertEquals(WalletTransactionConstants.TOP_UP, transactions.get(0).getType());
        assertMoney(AMOUNT_100000, transactions.get(0).getAvailableBalanceAfter());
        assertMoney(AMOUNT_0, transactions.get(0).getHeldBalanceAfter());
        assertEquals(WalletTransactionConstants.HOLD, transactions.get(1).getType());
        assertMoney(AMOUNT_75000, transactions.get(1).getAvailableBalanceAfter());
        assertMoney(AMOUNT_25000, transactions.get(1).getHeldBalanceAfter());
        assertEquals(WalletTransactionConstants.HOLD, transactions.get(2).getType());
        assertMoney(AMOUNT_60000, transactions.get(2).getAvailableBalanceAfter());
        assertMoney(AMOUNT_40000, transactions.get(2).getHeldBalanceAfter());
    }

    @Test
    void releaseForAuctionReleasesFullActiveHoldBackToAvailableBalance() {
        UUID userId = randomUserId();
        UUID auctionId = randomAuctionId();
        topUp(userId, AMOUNT_100000);
        holdForAuction(userId, auctionId, AMOUNT_25000);
        holdForAuction(userId, auctionId, AMOUNT_15000);

        HoldRecord releasedHold = walletService.releaseForAuction(userId, auctionId, new BigDecimal(AMOUNT_40000));

        assertBalance(walletService, userId, AMOUNT_100000, AMOUNT_0);
        assertHoldStatus(releasedHold, AMOUNT_40000, HoldRecord.HoldStatus.RELEASED);
    }

    @Test
    void releaseForAuctionRecordsAuctionReferenceAndEndingBalances() {
        UUID userId = randomUserId();
        UUID auctionId = randomAuctionId();
        topUp(userId, AMOUNT_100000);
        holdForAuction(userId, auctionId, AMOUNT_30000);
        transactionRepository.deleteAll();

        walletService.releaseForAuction(userId, auctionId, new BigDecimal(AMOUNT_30000));

        assertBalance(walletService, userId, AMOUNT_100000, AMOUNT_0);
        assertSingleTransaction(
                walletService,
                userId,
                WalletTransactionConstants.RELEASE,
                AMOUNT_30000,
                auctionId.toString(),
                AMOUNT_100000,
                AMOUNT_0
        );
    }

    @Test
    void releaseForAuctionThrowsWhenRequestedAmountDiffersFromActiveHoldAmount() {
        UUID userId = randomUserId();
        UUID auctionId = randomAuctionId();
        topUp(userId, AMOUNT_100000);
        holdForAuction(userId, auctionId, AMOUNT_40000);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> walletService.releaseForAuction(userId, auctionId, new BigDecimal(AMOUNT_30000))
        );

        assertEquals("Hold amount mismatch", exception.getMessage());
        assertBalance(walletService, userId, AMOUNT_60000, AMOUNT_40000);
    }

    @Test
    void releaseAndCaptureTransactionsStoreFinalAvailableBalance() {
        UUID userId = randomUserId();
        UUID auctionId = randomAuctionId();
        topUp(userId, AMOUNT_2000);
        holdForAuction(userId, auctionId, AMOUNT_1000);
        walletService.releaseForAuction(userId, auctionId, new BigDecimal(AMOUNT_1000));
        holdForAuction(userId, auctionId, AMOUNT_2000);
        walletService.captureForAuction(userId, auctionId, new BigDecimal(AMOUNT_2000));

        List<WalletTransaction> transactions = walletService.getTransactions(userId);
        assertEquals(WalletTransactionConstants.RELEASE, transactions.get(2).getType());
        assertMoney(AMOUNT_2000, transactions.get(2).getAvailableBalanceAfter());
        assertMoney(AMOUNT_0, transactions.get(2).getHeldBalanceAfter());
        assertEquals(WalletTransactionConstants.CAPTURE, transactions.get(4).getType());
        assertMoney(AMOUNT_0, transactions.get(4).getAvailableBalanceAfter());
        assertMoney(AMOUNT_0, transactions.get(4).getHeldBalanceAfter());
    }

    @Test
    void captureForAuctionCapturesFullActiveHoldWithoutReturningAvailableBalance() {
        UUID userId = randomUserId();
        UUID auctionId = randomAuctionId();
        topUp(userId, AMOUNT_100000);
        holdForAuction(userId, auctionId, AMOUNT_25000);
        holdForAuction(userId, auctionId, AMOUNT_15000);

        HoldRecord capturedHold = walletService.captureForAuction(userId, auctionId, new BigDecimal(AMOUNT_40000));

        assertBalance(walletService, userId, AMOUNT_60000, AMOUNT_0);
        assertHoldStatus(capturedHold, AMOUNT_40000, HoldRecord.HoldStatus.CAPTURED);
    }

    @Test
    void captureForAuctionRecordsAuctionReferenceAndEndingBalances() {
        UUID userId = randomUserId();
        UUID auctionId = randomAuctionId();
        topUp(userId, AMOUNT_100000);
        holdForAuction(userId, auctionId, AMOUNT_30000);
        transactionRepository.deleteAll();

        walletService.captureForAuction(userId, auctionId, new BigDecimal(AMOUNT_30000));

        assertBalance(walletService, userId, AMOUNT_70000, AMOUNT_0);
        assertSingleTransaction(
                walletService,
                userId,
                WalletTransactionConstants.CAPTURE,
                AMOUNT_30000,
                auctionId.toString(),
                AMOUNT_70000,
                AMOUNT_0
        );
    }

    @Test
    void creditForAuctionAddsAuctionPaymentToSellerAvailableBalance() {
        UUID sellerId = randomUserId();
        UUID auctionId = randomAuctionId();

        walletService.creditForAuction(
                sellerId,
                auctionId,
                new BigDecimal(AMOUNT_40000)
        );

        assertBalance(walletService, sellerId, AMOUNT_40000, AMOUNT_0);
    }

    @Test
    void creditForAuctionRecordsAuctionReferenceAndEndingBalances() {
        UUID userId = randomUserId();
        UUID auctionId = randomAuctionId();

        walletService.creditForAuction(
                userId,
                auctionId,
                new BigDecimal(AMOUNT_40000)
        );

        assertBalance(walletService, userId, AMOUNT_40000, AMOUNT_0);
        assertSingleTransaction(
                walletService,
                userId,
                WalletTransactionConstants.AUCTION_CREDIT,
                AMOUNT_40000,
                auctionId.toString(),
                AMOUNT_40000,
                AMOUNT_0
        );
    }

    @Test
    void captureForAuctionThrowsWhenRequestedAmountDiffersFromActiveHoldAmount() {
        UUID userId = randomUserId();
        UUID auctionId = randomAuctionId();
        topUp(userId, AMOUNT_100000);
        holdForAuction(userId, auctionId, AMOUNT_40000);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> walletService.captureForAuction(userId, auctionId, new BigDecimal(AMOUNT_30000))
        );

        assertEquals("Hold amount mismatch", exception.getMessage());
        assertBalance(walletService, userId, AMOUNT_60000, AMOUNT_40000);
    }

    @Test
    void holdForAuctionFailsWhenAvailableBalanceIsInsufficient() {
        UUID userId = randomUserId();
        UUID auctionId = randomAuctionId();
        topUp(userId, AMOUNT_10000);

        assertThrows(
                IllegalArgumentException.class,
                () -> walletService.holdForAuction(userId, auctionId, new BigDecimal(AMOUNT_25000))
        );

        assertBalance(walletService, userId, AMOUNT_10000, AMOUNT_0);
    }

    private UUID randomUserId() {
        return UUID.randomUUID();
    }

    private UUID randomAuctionId() {
        return UUID.randomUUID();
    }

    private void topUp(UUID userId, String amount) {
        walletService.topUp(userId, new BigDecimal(amount));
    }

    private HoldRecord holdForAuction(UUID userId, UUID auctionId, String amount) {
        return walletService.holdForAuction(userId, auctionId, new BigDecimal(amount));
    }

    private void assertHoldStatus(
            HoldRecord holdRecord,
            String expectedAmount,
            HoldRecord.HoldStatus expectedStatus
    ) {
        assertMoney(expectedAmount, holdRecord.getAmount());
        assertEquals(expectedStatus, holdRecord.getStatus());
    }

}
