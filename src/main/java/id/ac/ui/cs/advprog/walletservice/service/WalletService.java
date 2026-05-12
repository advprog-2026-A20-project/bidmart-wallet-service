package id.ac.ui.cs.advprog.walletservice.service;

import id.ac.ui.cs.advprog.walletservice.dto.TopUpRequest;
import id.ac.ui.cs.advprog.walletservice.dto.TransactionResponse;
import id.ac.ui.cs.advprog.walletservice.dto.WalletBalanceResponse;
import id.ac.ui.cs.advprog.walletservice.dto.WalletResponse;
import id.ac.ui.cs.advprog.walletservice.model.HoldRecord;
import id.ac.ui.cs.advprog.walletservice.model.Wallet;
import id.ac.ui.cs.advprog.walletservice.model.WalletTransaction;
import id.ac.ui.cs.advprog.walletservice.repository.HoldRepository;
import id.ac.ui.cs.advprog.walletservice.repository.TransactionRepository;
import id.ac.ui.cs.advprog.walletservice.repository.WalletRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

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

    public WalletResponse getPublicWallet(UUID userId) {
        Wallet wallet = walletRepository.findOrCreateByUserId(userId);
        return new WalletResponse(userId, wallet.getAvailableBalance(), userId);
    }

    public WalletResponse topUpPublic(UUID userId, TopUpRequest request) {
        if (request.amount() == null || request.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Amount must be greater than 0");
        }
        Wallet wallet = walletRepository.findOrCreateByUserId(userId);
        wallet.topUp(request.amount());
        transactionRepository.add(new WalletTransaction(userId, "TOPUP", request.amount(), "manual"));
        return getPublicWallet(userId);
    }

    public List<TransactionResponse> getPublicTransactions(UUID userId) {
        return transactionRepository.findByUserId(userId).stream()
            .map(t -> new TransactionResponse(
                t.getTransactionId(),
                t.getType(),
                t.getAmount(),
                walletRepository.findOrCreateByUserId(userId).getAvailableBalance(),
                t.getReference(),
                t.getTimestamp()
            ))
            .toList();
    }

    public WalletBalanceResponse topUp(UUID userId, BigDecimal amount) {
        Wallet wallet = walletRepository.findOrCreateByUserId(userId);
        wallet.topUp(amount);
        transactionRepository.add(new WalletTransaction(userId, "TOP_UP", amount, "manual"));
        return getBalance(userId);
    }

    public WalletBalanceResponse withdraw(UUID userId, BigDecimal amount) {
        Wallet wallet = walletRepository.findOrCreateByUserId(userId);
        if (wallet.getAvailableBalance().compareTo(amount) < 0) throw new ResponseStatusException(HttpStatus.CONFLICT, "Insufficient balance");
        wallet.withdraw(amount);
        transactionRepository.add(new WalletTransaction(userId, "WITHDRAW", amount, "manual"));
        return getBalance(userId);
    }

    public HoldRecord hold(UUID userId, BigDecimal amount, String idempotencyKey) {
        if (idempotencyKey != null && idempotencyCache.containsKey(idempotencyKey)) {
            return (HoldRecord) idempotencyCache.get(idempotencyKey);
        }
        Wallet wallet = walletRepository.findOrCreateByUserId(userId);
        if (wallet.getAvailableBalance().compareTo(amount) < 0) throw new ResponseStatusException(HttpStatus.CONFLICT, "Insufficient balance");
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
        HoldRecord holdRecord = holdRepository.findById(holdId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Hold not found"));
        if (!holdRecord.getUserId().equals(userId)) throw new ResponseStatusException(HttpStatus.CONFLICT, "Hold ownership mismatch");
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
        HoldRecord holdRecord = holdRepository.findById(holdId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Hold not found"));
        if (!holdRecord.getUserId().equals(userId)) throw new ResponseStatusException(HttpStatus.CONFLICT, "Hold ownership mismatch");
        if (holdRecord.getStatus() == HoldRecord.HoldStatus.HELD) {
            walletRepository.findOrCreateByUserId(userId).capture(holdRecord.getAmount());
            holdRecord.markCaptured();
            transactionRepository.add(new WalletTransaction(userId, "CAPTURE", holdRecord.getAmount(), holdId.toString()));
        }
        if (idempotencyKey != null) idempotencyCache.put(idempotencyKey, holdRecord);
        return holdRecord;
    }

    public void holdInternal(UUID userId, BigDecimal amount) {
        Wallet wallet = walletRepository.findOrCreateByUserId(userId);
        if (wallet.getAvailableBalance().compareTo(amount) < 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Insufficient balance for this bid");
        }
        wallet.hold(amount);
    }

    public void releaseInternal(UUID userId, BigDecimal amount) {
        Wallet wallet = walletRepository.findOrCreateByUserId(userId);
        wallet.release(amount);
    }

    public void captureInternal(UUID userId, BigDecimal amount) {
        Wallet wallet = walletRepository.findOrCreateByUserId(userId);
        wallet.capture(amount);
    }

    public List<WalletTransaction> getTransactions(UUID userId) {
        return transactionRepository.findByUserId(userId);
    }
}
