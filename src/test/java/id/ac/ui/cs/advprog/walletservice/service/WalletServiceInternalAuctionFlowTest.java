package id.ac.ui.cs.advprog.walletservice.service;

import id.ac.ui.cs.advprog.walletservice.dto.WalletBalanceResponse;
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
        UUID userId = UUID.randomUUID();
        UUID auctionId = UUID.randomUUID();
        walletService.topUp(userId, new BigDecimal(AMOUNT_100000));

        HoldRecord holdRecord = walletService.holdForAuction(userId, auctionId, new BigDecimal(AMOUNT_25000));

        WalletBalanceResponse balance = walletService.getBalance(userId);
        assertMoney(AMOUNT_75000, balance.availableBalance());
        assertMoney(AMOUNT_25000, balance.heldBalance());
        assertMoney(AMOUNT_25000, holdRecord.getAmount());
        assertEquals(HoldRecord.HoldStatus.HELD, holdRecord.getStatus());
    }

    @Test
    void holdForAuctionRecordsAuctionReferenceAndEndingBalances() {
        UUID userId = UUID.randomUUID();
        UUID auctionId = UUID.randomUUID();
        walletService.topUp(userId, new BigDecimal(AMOUNT_100000));
        transactionRepository.deleteAll();

        walletService.holdForAuction(userId, auctionId, new BigDecimal(AMOUNT_30000));

        WalletBalanceResponse balance = walletService.getBalance(userId);
        assertMoney(AMOUNT_70000, balance.availableBalance());
        assertMoney(AMOUNT_30000, balance.heldBalance());

        List<WalletTransaction> transactions = walletService.getTransactions(userId);
        assertEquals(1, transactions.size());

        WalletTransaction transaction = transactions.get(0);
        assertEquals(userId, transaction.getUserId());
        assertEquals(WalletTransactionConstants.HOLD, transaction.getType());
        assertMoney(AMOUNT_30000, transaction.getAmount());
        assertEquals(auctionId.toString(), transaction.getReference());
        assertMoney(AMOUNT_70000, transaction.getAvailableBalanceAfter());
        assertMoney(AMOUNT_30000, transaction.getHeldBalanceAfter());
    }

    @Test
    void holdForAuctionIncreasesExistingActiveHoldForSameUserAndAuction() {
        UUID userId = UUID.randomUUID();
        UUID auctionId = UUID.randomUUID();
        walletService.topUp(userId, new BigDecimal(AMOUNT_100000));
        HoldRecord firstHold = walletService.holdForAuction(userId, auctionId, new BigDecimal(AMOUNT_25000));

        HoldRecord secondHold = walletService.holdForAuction(userId, auctionId, new BigDecimal(AMOUNT_15000));

        WalletBalanceResponse balance = walletService.getBalance(userId);
        assertEquals(firstHold.getHoldId(), secondHold.getHoldId());
        assertMoney(AMOUNT_60000, balance.availableBalance());
        assertMoney(AMOUNT_40000, balance.heldBalance());
        assertMoney(AMOUNT_40000, secondHold.getAmount());
        assertEquals(HoldRecord.HoldStatus.HELD, secondHold.getStatus());

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
        UUID userId = UUID.randomUUID();
        UUID auctionId = UUID.randomUUID();
        walletService.topUp(userId, new BigDecimal(AMOUNT_100000));
        walletService.holdForAuction(userId, auctionId, new BigDecimal(AMOUNT_25000));
        walletService.holdForAuction(userId, auctionId, new BigDecimal(AMOUNT_15000));

        HoldRecord releasedHold = walletService.releaseForAuction(userId, auctionId, new BigDecimal(AMOUNT_40000));

        WalletBalanceResponse balance = walletService.getBalance(userId);
        assertMoney(AMOUNT_100000, balance.availableBalance());
        assertMoney(AMOUNT_0, balance.heldBalance());
        assertMoney(AMOUNT_40000, releasedHold.getAmount());
        assertEquals(HoldRecord.HoldStatus.RELEASED, releasedHold.getStatus());
    }

    @Test
    void releaseForAuctionRecordsAuctionReferenceAndEndingBalances() {
        UUID userId = UUID.randomUUID();
        UUID auctionId = UUID.randomUUID();
        walletService.topUp(userId, new BigDecimal(AMOUNT_100000));
        walletService.holdForAuction(userId, auctionId, new BigDecimal(AMOUNT_30000));
        transactionRepository.deleteAll();

        walletService.releaseForAuction(userId, auctionId, new BigDecimal(AMOUNT_30000));

        WalletBalanceResponse balance = walletService.getBalance(userId);
        assertMoney(AMOUNT_100000, balance.availableBalance());
        assertMoney(AMOUNT_0, balance.heldBalance());

        List<WalletTransaction> transactions = walletService.getTransactions(userId);
        assertEquals(1, transactions.size());

        WalletTransaction transaction = transactions.get(0);
        assertEquals(userId, transaction.getUserId());
        assertEquals(WalletTransactionConstants.RELEASE, transaction.getType());
        assertMoney(AMOUNT_30000, transaction.getAmount());
        assertEquals(auctionId.toString(), transaction.getReference());
        assertMoney(AMOUNT_100000, transaction.getAvailableBalanceAfter());
        assertMoney(AMOUNT_0, transaction.getHeldBalanceAfter());
    }

    @Test
    void releaseForAuctionThrowsWhenRequestedAmountDiffersFromActiveHoldAmount() {
        UUID userId = UUID.randomUUID();
        UUID auctionId = UUID.randomUUID();
        walletService.topUp(userId, new BigDecimal(AMOUNT_100000));
        walletService.holdForAuction(userId, auctionId, new BigDecimal(AMOUNT_40000));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> walletService.releaseForAuction(userId, auctionId, new BigDecimal(AMOUNT_30000))
        );

        WalletBalanceResponse balance = walletService.getBalance(userId);
        assertEquals("Hold amount mismatch", exception.getMessage());
        assertMoney(AMOUNT_60000, balance.availableBalance());
        assertMoney(AMOUNT_40000, balance.heldBalance());
    }

    @Test
    void releaseAndCaptureTransactionsStoreFinalAvailableBalance() {
        UUID userId = UUID.randomUUID();
        UUID auctionId = UUID.randomUUID();
        walletService.topUp(userId, new BigDecimal(AMOUNT_2000));
        walletService.holdForAuction(userId, auctionId, new BigDecimal(AMOUNT_1000));
        walletService.releaseForAuction(userId, auctionId, new BigDecimal(AMOUNT_1000));
        walletService.holdForAuction(userId, auctionId, new BigDecimal(AMOUNT_2000));
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
        UUID userId = UUID.randomUUID();
        UUID auctionId = UUID.randomUUID();
        walletService.topUp(userId, new BigDecimal(AMOUNT_100000));
        walletService.holdForAuction(userId, auctionId, new BigDecimal(AMOUNT_25000));
        walletService.holdForAuction(userId, auctionId, new BigDecimal(AMOUNT_15000));

        HoldRecord capturedHold = walletService.captureForAuction(userId, auctionId, new BigDecimal(AMOUNT_40000));

        WalletBalanceResponse balance = walletService.getBalance(userId);
        assertMoney(AMOUNT_60000, balance.availableBalance());
        assertMoney(AMOUNT_0, balance.heldBalance());
        assertMoney(AMOUNT_40000, capturedHold.getAmount());
        assertEquals(HoldRecord.HoldStatus.CAPTURED, capturedHold.getStatus());
    }

    @Test
    void captureForAuctionRecordsAuctionReferenceAndEndingBalances() {
        UUID userId = UUID.randomUUID();
        UUID auctionId = UUID.randomUUID();
        walletService.topUp(userId, new BigDecimal(AMOUNT_100000));
        walletService.holdForAuction(userId, auctionId, new BigDecimal(AMOUNT_30000));
        transactionRepository.deleteAll();

        walletService.captureForAuction(userId, auctionId, new BigDecimal(AMOUNT_30000));

        WalletBalanceResponse balance = walletService.getBalance(userId);
        assertMoney(AMOUNT_70000, balance.availableBalance());
        assertMoney(AMOUNT_0, balance.heldBalance());

        List<WalletTransaction> transactions = walletService.getTransactions(userId);
        assertEquals(1, transactions.size());

        WalletTransaction transaction = transactions.get(0);
        assertEquals(userId, transaction.getUserId());
        assertEquals(WalletTransactionConstants.CAPTURE, transaction.getType());
        assertMoney(AMOUNT_30000, transaction.getAmount());
        assertEquals(auctionId.toString(), transaction.getReference());
        assertMoney(AMOUNT_70000, transaction.getAvailableBalanceAfter());
        assertMoney(AMOUNT_0, transaction.getHeldBalanceAfter());
    }

    @Test
    void creditForAuctionAddsAuctionPaymentToSellerAvailableBalance() {
        UUID sellerId = UUID.randomUUID();
        UUID auctionId = UUID.randomUUID();

        WalletBalanceResponse balance = walletService.creditForAuction(
                sellerId,
                auctionId,
                new BigDecimal(AMOUNT_40000)
        );

        assertMoney(AMOUNT_40000, balance.availableBalance());
        assertMoney(AMOUNT_0, balance.heldBalance());
    }

    @Test
    void creditForAuctionRecordsAuctionReferenceAndEndingBalances() {
        UUID userId = UUID.randomUUID();
        UUID auctionId = UUID.randomUUID();

        WalletBalanceResponse balance = walletService.creditForAuction(
                userId,
                auctionId,
                new BigDecimal(AMOUNT_40000)
        );

        assertMoney(AMOUNT_40000, balance.availableBalance());
        assertMoney(AMOUNT_0, balance.heldBalance());

        List<WalletTransaction> transactions = walletService.getTransactions(userId);
        assertEquals(1, transactions.size());

        WalletTransaction transaction = transactions.get(0);
        assertEquals(userId, transaction.getUserId());
        assertEquals(WalletTransactionConstants.AUCTION_CREDIT, transaction.getType());
        assertMoney(AMOUNT_40000, transaction.getAmount());
        assertEquals(auctionId.toString(), transaction.getReference());
        assertMoney(AMOUNT_40000, transaction.getAvailableBalanceAfter());
        assertMoney(AMOUNT_0, transaction.getHeldBalanceAfter());
    }

    @Test
    void captureForAuctionThrowsWhenRequestedAmountDiffersFromActiveHoldAmount() {
        UUID userId = UUID.randomUUID();
        UUID auctionId = UUID.randomUUID();
        walletService.topUp(userId, new BigDecimal(AMOUNT_100000));
        walletService.holdForAuction(userId, auctionId, new BigDecimal(AMOUNT_40000));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> walletService.captureForAuction(userId, auctionId, new BigDecimal(AMOUNT_30000))
        );

        WalletBalanceResponse balance = walletService.getBalance(userId);
        assertEquals("Hold amount mismatch", exception.getMessage());
        assertMoney(AMOUNT_60000, balance.availableBalance());
        assertMoney(AMOUNT_40000, balance.heldBalance());
    }

    @Test
    void holdForAuctionFailsWhenAvailableBalanceIsInsufficient() {
        UUID userId = UUID.randomUUID();
        UUID auctionId = UUID.randomUUID();
        walletService.topUp(userId, new BigDecimal(AMOUNT_10000));

        assertThrows(
                IllegalArgumentException.class,
                () -> walletService.holdForAuction(userId, auctionId, new BigDecimal(AMOUNT_25000))
        );

        WalletBalanceResponse balance = walletService.getBalance(userId);
        assertMoney(AMOUNT_10000, balance.availableBalance());
        assertMoney(AMOUNT_0, balance.heldBalance());
    }

    private void assertMoney(String expected, BigDecimal actual) {
        assertEquals(0, actual.compareTo(new BigDecimal(expected)));
    }
}
