package id.ac.ui.cs.advprog.walletservice.service;

import id.ac.ui.cs.advprog.walletservice.model.WalletTransactionConstants;
import id.ac.ui.cs.advprog.walletservice.repository.HoldRepository;
import id.ac.ui.cs.advprog.walletservice.repository.TransactionRepository;
import id.ac.ui.cs.advprog.walletservice.repository.WalletRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.UUID;

import static id.ac.ui.cs.advprog.walletservice.service.WalletServiceTestAssertions.assertBalance;
import static id.ac.ui.cs.advprog.walletservice.service.WalletServiceTestAssertions.assertSingleTransaction;

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

        walletService.topUp(userId, amount);

        assertBalance(walletService, userId, AMOUNT_10000, AMOUNT_0);
        assertSingleTransaction(
                walletService,
                userId,
                WalletTransactionConstants.TOP_UP,
                AMOUNT_10000,
                WalletTransactionConstants.MANUAL_REFERENCE,
                AMOUNT_10000,
                AMOUNT_0
        );
    }

    @Test
    void withdrawRecordsManualTransactionWithEndingBalances() {
        UUID userId = UUID.randomUUID();
        walletService.topUp(userId, new BigDecimal(AMOUNT_10000));
        transactionRepository.deleteAll();

        walletService.withdraw(userId, new BigDecimal(AMOUNT_3000));

        assertBalance(walletService, userId, AMOUNT_7000, AMOUNT_0);
        assertSingleTransaction(
                walletService,
                userId,
                WalletTransactionConstants.WITHDRAW,
                AMOUNT_3000,
                WalletTransactionConstants.MANUAL_REFERENCE,
                AMOUNT_7000,
                AMOUNT_0
        );
    }
}
