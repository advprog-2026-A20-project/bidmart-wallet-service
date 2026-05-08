package id.ac.ui.cs.advprog.walletservice.repository;

import id.ac.ui.cs.advprog.walletservice.model.Wallet;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class WalletRepository {
    private final Map<UUID, Wallet> wallets = new ConcurrentHashMap<>();

    public Wallet findOrCreateByUserId(UUID userId) {
        return wallets.computeIfAbsent(userId, Wallet::new);
    }
}
