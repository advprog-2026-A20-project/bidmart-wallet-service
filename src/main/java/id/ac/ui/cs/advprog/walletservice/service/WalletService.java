package id.ac.ui.cs.advprog.walletservice.service;

import id.ac.ui.cs.advprog.walletservice.dto.WalletBalanceResponse;
import id.ac.ui.cs.advprog.walletservice.model.HoldRecord;
import id.ac.ui.cs.advprog.walletservice.model.Wallet;
import id.ac.ui.cs.advprog.walletservice.model.WalletTransaction;
import id.ac.ui.cs.advprog.walletservice.repository.HoldRepository;
import id.ac.ui.cs.advprog.walletservice.repository.TransactionRepository;
import id.ac.ui.cs.advprog.walletservice.repository.WalletRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class WalletService {
    private final WalletRepository walletRepository;
    private final HoldRepository holdRepository;
    private final TransactionRepository transactionRepository;
    private final Map<String, Object> idempotencyCache = new ConcurrentHashMap<>();

    public WalletService(WalletRepository walletRepository, HoldRepository holdRepository, TransactionRepository transactionRepository) {
        this.walletRepository = walletRepository;
        this.holdRepository = holdRepository;
        this.transactionRepository = transactionRepository;
    }

    public WalletBalanceResponse getBalance(UUID userId) {
        Wallet wallet = walletRepository.findOrCreateByUserId(userId);
        return new WalletBalanceResponse(userId, wallet.getAvailableBalance(), wallet.getHeldBalance());
    }

    public WalletBalanceResponse topUp(UUID userId, BigDecimal amount) {
        Wallet wallet = walletRepository.findOrCreateByUserId(userId);
        wallet.topUp(amount);
        transactionRepository.add(new WalletTransaction(userId, "TOP_UP", amount, "manual"));
        return getBalance(userId);
    }

    public WalletBalanceResponse withdraw(UUID userId, BigDecimal amount) {
        Wallet wallet = walletRepository.findOrCreateByUserId(userId);
        wallet.withdraw(amount);
        transactionRepository.add(new WalletTransaction(userId, "WITHDRAW", amount, "manual"));
        return getBalance(userId);
    }

    public HoldRecord hold(UUID userId, BigDecimal amount, String idempotencyKey) {
        if (idempotencyKey != null && idempotencyCache.containsKey(idempotencyKey)) {
            return (HoldRecord) idempotencyCache.get(idempotencyKey);
        }
        Wallet wallet = walletRepository.findOrCreateByUserId(userId);
        wallet.hold(amount);
        HoldRecord holdRecord = holdRepository.save(new HoldRecord(UUID.randomUUID(), userId, amount));
        transactionRepository.add(new WalletTransaction(userId, "HOLD", amount, holdRecord.getHoldId().toString()));
        if (idempotencyKey != null) idempotencyCache.put(idempotencyKey, holdRecord);
        return holdRecord;
    }

    public HoldRecord release(UUID userId, UUID holdId, String idempotencyKey) {
        if (idempotencyKey != null && idempotencyCache.containsKey(idempotencyKey)) {
            return (HoldRecord) idempotencyCache.get(idempotencyKey);
        }
        HoldRecord holdRecord = holdRepository.findById(holdId).orElseThrow();
        if (!holdRecord.getUserId().equals(userId)) throw new IllegalArgumentException("Hold ownership mismatch");
        if (holdRecord.getStatus() == HoldRecord.HoldStatus.HELD) {
            walletRepository.findOrCreateByUserId(userId).release(holdRecord.getAmount());
            holdRecord.markReleased();
            transactionRepository.add(new WalletTransaction(userId, "RELEASE", holdRecord.getAmount(), holdId.toString()));
        }
        if (idempotencyKey != null) idempotencyCache.put(idempotencyKey, holdRecord);
        return holdRecord;
    }

    public HoldRecord capture(UUID userId, UUID holdId, String idempotencyKey) {
        if (idempotencyKey != null && idempotencyCache.containsKey(idempotencyKey)) {
            return (HoldRecord) idempotencyCache.get(idempotencyKey);
        }
        HoldRecord holdRecord = holdRepository.findById(holdId).orElseThrow();
        if (!holdRecord.getUserId().equals(userId)) throw new IllegalArgumentException("Hold ownership mismatch");
        if (holdRecord.getStatus() == HoldRecord.HoldStatus.HELD) {
            walletRepository.findOrCreateByUserId(userId).capture(holdRecord.getAmount());
            holdRecord.markCaptured();
            transactionRepository.add(new WalletTransaction(userId, "CAPTURE", holdRecord.getAmount(), holdId.toString()));
        }
        if (idempotencyKey != null) idempotencyCache.put(idempotencyKey, holdRecord);
        return holdRecord;
    }

    public synchronized HoldRecord holdForAuction(UUID userId, UUID auctionId, BigDecimal amount) {
        Wallet wallet = walletRepository.findOrCreateByUserId(userId);
        wallet.hold(amount);

        HoldRecord holdRecord = holdRepository.findActiveByUserIdAndAuctionId(userId, auctionId)
                .map(existingHold -> {
                    existingHold.increaseAmount(amount);
                    return existingHold;
                })
                .orElseGet(() -> holdRepository.save(new HoldRecord(UUID.randomUUID(), userId, auctionId, amount)));

        transactionRepository.add(new WalletTransaction(userId, "HOLD", amount, auctionId.toString()));
        return holdRecord;
    }

    public synchronized HoldRecord releaseForAuction(UUID userId, UUID auctionId, BigDecimal amount) {
        HoldRecord holdRecord = findActiveAuctionHold(userId, auctionId);
        walletRepository.findOrCreateByUserId(userId).release(holdRecord.getAmount());
        holdRecord.markReleased();
        transactionRepository.add(new WalletTransaction(userId, "RELEASE", holdRecord.getAmount(), auctionId.toString()));
        return holdRecord;
    }

    public synchronized HoldRecord captureForAuction(UUID userId, UUID auctionId, BigDecimal amount) {
        HoldRecord holdRecord = findActiveAuctionHold(userId, auctionId);
        walletRepository.findOrCreateByUserId(userId).capture(holdRecord.getAmount());
        holdRecord.markCaptured();
        transactionRepository.add(new WalletTransaction(userId, "CAPTURE", holdRecord.getAmount(), auctionId.toString()));
        return holdRecord;
    }

    public List<WalletTransaction> getTransactions(UUID userId) {
        return transactionRepository.findByUserId(userId);
    }

    private HoldRecord findActiveAuctionHold(UUID userId, UUID auctionId) {
        return holdRepository.findActiveByUserIdAndAuctionId(userId, auctionId)
                .orElseThrow(() -> new IllegalArgumentException("Active hold not found for auction"));
    }
}
