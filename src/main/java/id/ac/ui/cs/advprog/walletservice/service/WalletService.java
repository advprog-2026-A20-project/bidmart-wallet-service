package id.ac.ui.cs.advprog.walletservice.service;

import id.ac.ui.cs.advprog.walletservice.dto.WalletBalanceResponse;
import id.ac.ui.cs.advprog.walletservice.model.HoldRecord;
import id.ac.ui.cs.advprog.walletservice.model.Wallet;
import id.ac.ui.cs.advprog.walletservice.model.WalletTransaction;
import id.ac.ui.cs.advprog.walletservice.model.WalletTransactionConstants;
import id.ac.ui.cs.advprog.walletservice.repository.HoldRepository;
import id.ac.ui.cs.advprog.walletservice.repository.WalletRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class WalletService {
    private final WalletRepository walletRepository;
    private final HoldRepository holdRepository;
    private final WalletTransactionService walletTransactionService;
    private final Map<String, Object> idempotencyCache = new ConcurrentHashMap<>();

    public WalletService(WalletRepository walletRepository, HoldRepository holdRepository, WalletTransactionService walletTransactionService) {
        this.walletRepository = walletRepository;
        this.holdRepository = holdRepository;
        this.walletTransactionService = walletTransactionService;
    }

    @Transactional
    public WalletBalanceResponse getBalance(UUID userId) {
        Wallet wallet = findOrCreateWallet(userId);
        return new WalletBalanceResponse(userId, wallet.getAvailableBalance(), wallet.getHeldBalance());
    }

    @Transactional
    public WalletBalanceResponse topUp(UUID userId, BigDecimal amount) {
        Wallet wallet = findOrCreateWallet(userId);
        wallet.topUp(amount);
        walletTransactionService.recordTransaction(wallet, WalletTransactionConstants.TOP_UP, amount, WalletTransactionConstants.MANUAL_REFERENCE);
        return getBalance(userId);
    }

    @Transactional
    public WalletBalanceResponse withdraw(UUID userId, BigDecimal amount) {
        Wallet wallet = findOrCreateWallet(userId);
        wallet.withdraw(amount);
        walletTransactionService.recordTransaction(wallet, WalletTransactionConstants.WITHDRAW, amount, WalletTransactionConstants.MANUAL_REFERENCE);
        return getBalance(userId);
    }

    @Transactional
    public HoldRecord hold(UUID userId, BigDecimal amount, String idempotencyKey) {
        if (idempotencyKey != null && idempotencyCache.containsKey(idempotencyKey)) {
            return (HoldRecord) idempotencyCache.get(idempotencyKey);
        }
        Wallet wallet = findOrCreateWallet(userId);
        wallet.hold(amount);
        HoldRecord holdRecord = holdRepository.save(new HoldRecord(UUID.randomUUID(), userId, amount));
        walletTransactionService.recordTransaction(wallet, WalletTransactionConstants.HOLD, amount, holdRecord.getHoldId().toString());
        if (idempotencyKey != null) idempotencyCache.put(idempotencyKey, holdRecord);
        return holdRecord;
    }

    @Transactional
    public HoldRecord release(UUID userId, UUID holdId, String idempotencyKey) {
        if (idempotencyKey != null && idempotencyCache.containsKey(idempotencyKey)) {
            return (HoldRecord) idempotencyCache.get(idempotencyKey);
        }
        HoldRecord holdRecord = holdRepository.findByHoldId(holdId).orElseThrow();
        if (!holdRecord.getUserId().equals(userId)) throw new IllegalArgumentException("Hold ownership mismatch");
        if (holdRecord.getStatus() == HoldRecord.HoldStatus.HELD) {
            Wallet wallet = findOrCreateWallet(userId);
            wallet.release(holdRecord.getAmount());
            holdRecord.markReleased();
            walletTransactionService.recordTransaction(wallet, WalletTransactionConstants.RELEASE, holdRecord.getAmount(), holdId.toString());
        }
        if (idempotencyKey != null) idempotencyCache.put(idempotencyKey, holdRecord);
        return holdRecord;
    }

    @Transactional
    public HoldRecord capture(UUID userId, UUID holdId, String idempotencyKey) {
        if (idempotencyKey != null && idempotencyCache.containsKey(idempotencyKey)) {
            return (HoldRecord) idempotencyCache.get(idempotencyKey);
        }
        HoldRecord holdRecord = holdRepository.findByHoldId(holdId).orElseThrow();
        if (!holdRecord.getUserId().equals(userId)) throw new IllegalArgumentException("Hold ownership mismatch");
        if (holdRecord.getStatus() == HoldRecord.HoldStatus.HELD) {
            Wallet wallet = findOrCreateWallet(userId);
            wallet.capture(holdRecord.getAmount());
            holdRecord.markCaptured();
            walletTransactionService.recordTransaction(wallet, WalletTransactionConstants.CAPTURE, holdRecord.getAmount(), holdId.toString());
        }
        if (idempotencyKey != null) idempotencyCache.put(idempotencyKey, holdRecord);
        return holdRecord;
    }

    @Transactional
    public synchronized HoldRecord holdForAuction(UUID userId, UUID auctionId, BigDecimal amount) {
        Wallet wallet = findOrCreateWallet(userId);
        wallet.hold(amount);

        HoldRecord holdRecord = holdRepository.findFirstByUserIdAndAuctionIdAndStatus(userId, auctionId, HoldRecord.HoldStatus.HELD)
                .map(existingHold -> {
                    existingHold.increaseAmount(amount);
                    return existingHold;
                })
                .orElseGet(() -> holdRepository.save(new HoldRecord(UUID.randomUUID(), userId, auctionId, amount)));

        walletTransactionService.recordTransaction(wallet, WalletTransactionConstants.HOLD, amount, auctionId.toString());
        return holdRecord;
    }

    @Transactional
    public synchronized HoldRecord releaseForAuction(UUID userId, UUID auctionId, BigDecimal amount) {
        HoldRecord holdRecord = findActiveAuctionHold(userId, auctionId);
        validateRequestedHoldAmount(holdRecord, amount);
        Wallet wallet = findOrCreateWallet(userId);
        wallet.release(holdRecord.getAmount());
        holdRecord.markReleased();
        walletTransactionService.recordTransaction(wallet, WalletTransactionConstants.RELEASE, holdRecord.getAmount(), auctionId.toString());
        return holdRecord;
    }

    @Transactional
    public synchronized HoldRecord captureForAuction(UUID userId, UUID auctionId, BigDecimal amount) {
        HoldRecord holdRecord = findActiveAuctionHold(userId, auctionId);
        validateRequestedHoldAmount(holdRecord, amount);
        Wallet wallet = findOrCreateWallet(userId);
        wallet.capture(holdRecord.getAmount());
        holdRecord.markCaptured();
        walletTransactionService.recordTransaction(wallet, WalletTransactionConstants.CAPTURE, holdRecord.getAmount(), auctionId.toString());
        return holdRecord;
    }

    @Transactional
    public synchronized WalletBalanceResponse creditForAuction(UUID userId, UUID auctionId, BigDecimal amount) {
        Wallet wallet = findOrCreateWallet(userId);
        wallet.topUp(amount);
        walletTransactionService.recordTransaction(wallet, WalletTransactionConstants.AUCTION_CREDIT, amount, auctionId.toString());
        return getBalance(userId);
    }

    @Transactional(readOnly = true)
    public List<WalletTransaction> getTransactions(UUID userId) {
        return walletTransactionService.getTransactions(userId);
    }

    private HoldRecord findActiveAuctionHold(UUID userId, UUID auctionId) {
        return holdRepository.findFirstByUserIdAndAuctionIdAndStatus(userId, auctionId, HoldRecord.HoldStatus.HELD)
                .orElseThrow(() -> new IllegalArgumentException("Active hold not found for auction"));
    }

    private void validateRequestedHoldAmount(HoldRecord holdRecord, BigDecimal amount) {
        if (holdRecord.getAmount().compareTo(amount) != 0) {
            throw new IllegalArgumentException("Hold amount mismatch");
        }
    }

    private Wallet findOrCreateWallet(UUID userId) {
        return walletRepository.findByUserIdForUpdate(userId)
                .orElseGet(() -> walletRepository.saveAndFlush(new Wallet(userId)));
    }

}
