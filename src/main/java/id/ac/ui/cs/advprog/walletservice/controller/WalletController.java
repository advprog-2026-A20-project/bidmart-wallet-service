package id.ac.ui.cs.advprog.walletservice.controller;

import id.ac.ui.cs.advprog.walletservice.dto.AmountRequest;
import id.ac.ui.cs.advprog.walletservice.dto.HoldResponse;
import id.ac.ui.cs.advprog.walletservice.dto.WalletBalanceResponse;
import id.ac.ui.cs.advprog.walletservice.model.HoldRecord;
import id.ac.ui.cs.advprog.walletservice.model.WalletTransaction;
import id.ac.ui.cs.advprog.walletservice.service.WalletService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/wallets")
public class WalletController {
    private final WalletService walletService;

    public WalletController(WalletService walletService) {
        this.walletService = walletService;
    }

    @GetMapping("/{userId}/balance")
    public WalletBalanceResponse getBalance(@PathVariable UUID userId) {
        return walletService.getBalance(userId);
    }

    @PostMapping("/{userId}/top-up")
    public WalletBalanceResponse topUp(@PathVariable UUID userId, @Valid @RequestBody AmountRequest request) {
        return walletService.topUp(userId, request.amount());
    }

    @PostMapping("/{userId}/withdraw")
    public WalletBalanceResponse withdraw(@PathVariable UUID userId, @Valid @RequestBody AmountRequest request) {
        return walletService.withdraw(userId, request.amount());
    }

    @PostMapping("/{userId}/holds")
    public HoldResponse hold(@PathVariable UUID userId, @Valid @RequestBody AmountRequest request) {
        return toResponse(walletService.hold(userId, request.amount(), request.idempotencyKey()));
    }

    @PostMapping("/{userId}/holds/{holdId}/release")
    public HoldResponse release(
            @PathVariable UUID userId,
            @PathVariable UUID holdId,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey
    ) {
        return toResponse(walletService.release(userId, holdId, idempotencyKey));
    }

    @PostMapping("/{userId}/holds/{holdId}/capture")
    public HoldResponse capture(
            @PathVariable UUID userId,
            @PathVariable UUID holdId,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey
    ) {
        return toResponse(walletService.capture(userId, holdId, idempotencyKey));
    }

    @GetMapping("/{userId}/transactions")
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
