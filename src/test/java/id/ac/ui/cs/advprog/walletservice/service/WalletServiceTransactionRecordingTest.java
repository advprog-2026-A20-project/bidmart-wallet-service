package id.ac.ui.cs.advprog.walletservice.service;

import id.ac.ui.cs.advprog.walletservice.dto.WalletBalanceResponse;
import id.ac.ui.cs.advprog.walletservice.model.WalletTransaction;
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
        BigDecimal amount = new BigDecimal("10000");

        WalletBalanceResponse balance = walletService.topUp(userId, amount);

        assertMoney("10000", balance.availableBalance());
        assertMoney("0", balance.heldBalance());

        List<WalletTransaction> transactions = walletService.getTransactions(userId);
        assertEquals(1, transactions.size());

        WalletTransaction transaction = transactions.get(0);
        assertEquals(userId, transaction.getUserId());
        assertEquals("TOP_UP", transaction.getType());
        assertMoney("10000", transaction.getAmount());
        assertEquals("manual", transaction.getReference());
        assertMoney("10000", transaction.getAvailableBalanceAfter());
        assertMoney("0", transaction.getHeldBalanceAfter());
    }

    @Test
    void withdrawRecordsManualTransactionWithEndingBalances() {
        UUID userId = UUID.randomUUID();
        walletService.topUp(userId, new BigDecimal("10000"));
        transactionRepository.deleteAll();

        WalletBalanceResponse balance = walletService.withdraw(userId, new BigDecimal("3000"));

        assertMoney("7000", balance.availableBalance());
        assertMoney("0", balance.heldBalance());

        List<WalletTransaction> transactions = walletService.getTransactions(userId);
        assertEquals(1, transactions.size());

        WalletTransaction transaction = transactions.get(0);
        assertEquals(userId, transaction.getUserId());
        assertEquals("WITHDRAW", transaction.getType());
        assertMoney("3000", transaction.getAmount());
        assertEquals("manual", transaction.getReference());
        assertMoney("7000", transaction.getAvailableBalanceAfter());
        assertMoney("0", transaction.getHeldBalanceAfter());
    }

    private void assertMoney(String expected, BigDecimal actual) {
        assertEquals(0, actual.compareTo(new BigDecimal(expected)));
    }
}
