package id.ac.ui.cs.advprog.walletservice.repository;

import id.ac.ui.cs.advprog.walletservice.model.WalletTransaction;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class TransactionRepository {
    private final Map<UUID, List<WalletTransaction>> transactionsByUser = new ConcurrentHashMap<>();

    public void add(WalletTransaction transaction) {
        transactionsByUser
                .computeIfAbsent(transaction.getUserId(), ignored -> new ArrayList<>())
                .add(transaction);
    }

    public List<WalletTransaction> findByUserId(UUID userId) {
        return transactionsByUser.getOrDefault(userId, List.of());
    }
}
