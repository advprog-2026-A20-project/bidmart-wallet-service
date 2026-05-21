package id.ac.ui.cs.advprog.walletservice.controller;

import id.ac.ui.cs.advprog.walletservice.dto.AmountRequest;
import id.ac.ui.cs.advprog.walletservice.dto.HoldResponse;
import id.ac.ui.cs.advprog.walletservice.dto.TopUpRequest;
import id.ac.ui.cs.advprog.walletservice.dto.TransactionResponse;
import id.ac.ui.cs.advprog.walletservice.dto.WalletBalanceResponse;
import id.ac.ui.cs.advprog.walletservice.dto.WalletResponse;
import id.ac.ui.cs.advprog.walletservice.model.HoldRecord;
import id.ac.ui.cs.advprog.walletservice.model.WalletTransaction;
import id.ac.ui.cs.advprog.walletservice.security.AuthenticatedUser;
import id.ac.ui.cs.advprog.walletservice.service.WalletService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class WalletController {
    private final WalletService walletService;

    public WalletController(WalletService walletService) {
        this.walletService = walletService;
    }

    @GetMapping("/wallet/balance")
    public WalletResponse getBalanceCompat(@AuthenticationPrincipal AuthenticatedUser authenticatedUser) {
        return walletService.getPublicWallet(authenticatedUser.id());
    }

    @PostMapping("/wallet/topup")
    public WalletResponse topUpCompat(
        @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
        @RequestBody TopUpRequest request
    ) {
        return walletService.topUpPublic(authenticatedUser.id(), request);
    }

    @GetMapping("/wallet/transactions")
    public List<TransactionResponse> transactionsCompat(@AuthenticationPrincipal AuthenticatedUser authenticatedUser) {
        return walletService.getPublicTransactions(authenticatedUser.id());
    }

    @GetMapping("/wallets/{userId}/balance")
    public WalletBalanceResponse getBalance(@PathVariable UUID userId) {
        return walletService.getBalance(userId);
    }

    @PostMapping("/wallets/{userId}/top-up")
    public WalletBalanceResponse topUp(@PathVariable UUID userId, @Valid @RequestBody AmountRequest request) {
        return walletService.topUp(userId, request.amount());
    }

    @PostMapping("/wallets/{userId}/withdraw")
    public WalletBalanceResponse withdraw(@PathVariable UUID userId, @Valid @RequestBody AmountRequest request) {
        return walletService.withdraw(userId, request.amount());
    }

    @PostMapping("/wallets/{userId}/holds")
    public HoldResponse hold(@PathVariable UUID userId, @Valid @RequestBody AmountRequest request) {
        return toResponse(walletService.hold(userId, request.amount(), request.idempotencyKey()));
    }

    @PostMapping("/wallets/{userId}/holds/{holdId}/release")
    public HoldResponse release(
            @PathVariable UUID userId,
            @PathVariable UUID holdId,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey
    ) {
        return toResponse(walletService.release(userId, holdId, idempotencyKey));
    }

    @PostMapping("/wallets/{userId}/holds/{holdId}/capture")
    public HoldResponse capture(
            @PathVariable UUID userId,
            @PathVariable UUID holdId,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey
    ) {
        return toResponse(walletService.capture(userId, holdId, idempotencyKey));
    }

    @GetMapping("/wallets/{userId}/transactions")
    public List<WalletTransaction> transactions(@PathVariable UUID userId) {
        return walletService.getTransactions(userId);
    }

    private HoldResponse toResponse(HoldRecord holdRecord) {
        return new HoldResponse(
                holdRecord.getHoldId(),
                holdRecord.getUserId(),
                holdRecord.getAmount(),
                holdRecord.getStatus().name()
        );
    }
}
