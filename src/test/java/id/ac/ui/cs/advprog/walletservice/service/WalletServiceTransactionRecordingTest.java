package id.ac.ui.cs.advprog.walletservice.service;

import id.ac.ui.cs.advprog.walletservice.dto.WalletBalanceResponse;
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

@SpringBootTest
class WalletServiceTransactionRecordingTest {
    private static final String AMOUNT_0 = "0";
    private static final String AMOUNT_3000 = "3000";
    private static final String AMOUNT_7000 = "7000";
    private static final String AMOUNT_10000 = "10000";

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
    void topUpRecordsManualTransactionWithEndingBalances() {
        UUID userId = UUID.randomUUID();
        BigDecimal amount = new BigDecimal(AMOUNT_10000);

        WalletBalanceResponse balance = walletService.topUp(userId, amount);

        assertMoney(AMOUNT_10000, balance.availableBalance());
        assertMoney(AMOUNT_0, balance.heldBalance());

        List<WalletTransaction> transactions = walletService.getTransactions(userId);
        assertEquals(1, transactions.size());

        WalletTransaction transaction = transactions.get(0);
        assertEquals(userId, transaction.getUserId());
        assertEquals(WalletTransactionConstants.TOP_UP, transaction.getType());
        assertMoney(AMOUNT_10000, transaction.getAmount());
        assertEquals(WalletTransactionConstants.MANUAL_REFERENCE, transaction.getReference());
        assertMoney(AMOUNT_10000, transaction.getAvailableBalanceAfter());
        assertMoney(AMOUNT_0, transaction.getHeldBalanceAfter());
    }

    @Test
    void withdrawRecordsManualTransactionWithEndingBalances() {
        UUID userId = UUID.randomUUID();
        walletService.topUp(userId, new BigDecimal(AMOUNT_10000));
        transactionRepository.deleteAll();

        WalletBalanceResponse balance = walletService.withdraw(userId, new BigDecimal(AMOUNT_3000));

        assertMoney(AMOUNT_7000, balance.availableBalance());
        assertMoney(AMOUNT_0, balance.heldBalance());

        List<WalletTransaction> transactions = walletService.getTransactions(userId);
        assertEquals(1, transactions.size());

        WalletTransaction transaction = transactions.get(0);
        assertEquals(userId, transaction.getUserId());
        assertEquals(WalletTransactionConstants.WITHDRAW, transaction.getType());
        assertMoney(AMOUNT_3000, transaction.getAmount());
        assertEquals(WalletTransactionConstants.MANUAL_REFERENCE, transaction.getReference());
        assertMoney(AMOUNT_7000, transaction.getAvailableBalanceAfter());
        assertMoney(AMOUNT_0, transaction.getHeldBalanceAfter());
    }

    private void assertMoney(String expected, BigDecimal actual) {
        assertEquals(0, actual.compareTo(new BigDecimal(expected)));
    }
}
