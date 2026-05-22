package id.ac.ui.cs.advprog.walletservice.service;

import id.ac.ui.cs.advprog.walletservice.model.Wallet;
import id.ac.ui.cs.advprog.walletservice.model.WalletTransaction;
import id.ac.ui.cs.advprog.walletservice.model.WalletTransactionFactory;
import id.ac.ui.cs.advprog.walletservice.repository.TransactionRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WalletTransactionService {
    private static final Logger logger = LoggerFactory.getLogger(WalletTransactionService.class);

    private final TransactionRepository transactionRepository;

    public WalletTransactionService(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    @Transactional
    public void recordTransaction(Wallet wallet, String type, BigDecimal amount, String reference) {
        WalletTransaction transaction = WalletTransactionFactory.createFrom(wallet, type, amount, reference);
        transactionRepository.save(transaction);
        logger.info("Recorded wallet transaction: userId={}, type={}, reference={}", wallet.getUserId(), type, reference);
    }

    @Transactional(readOnly = true)
    public List<WalletTransaction> getTransactions(UUID userId) {
        return transactionRepository.findByUserIdOrderByTimestampAsc(userId);
    }
}
