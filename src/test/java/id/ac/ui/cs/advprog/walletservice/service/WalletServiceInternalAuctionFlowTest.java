package id.ac.ui.cs.advprog.walletservice.service;

import id.ac.ui.cs.advprog.walletservice.dto.WalletBalanceResponse;
import id.ac.ui.cs.advprog.walletservice.model.HoldRecord;
import id.ac.ui.cs.advprog.walletservice.repository.HoldRepository;
import id.ac.ui.cs.advprog.walletservice.repository.TransactionRepository;
import id.ac.ui.cs.advprog.walletservice.repository.WalletRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class WalletServiceInternalAuctionFlowTest {

    private final WalletService walletService = new WalletService(
            new WalletRepository(),
            new HoldRepository(),
            new TransactionRepository()
    );

    @Test
    void holdForAuctionMovesAmountFromAvailableBalanceToHeldBalance() {
        UUID userId = UUID.randomUUID();
        UUID auctionId = UUID.randomUUID();
        walletService.topUp(userId, new BigDecimal("100000"));

        HoldRecord holdRecord = walletService.holdForAuction(userId, auctionId, new BigDecimal("25000"));

        WalletBalanceResponse balance = walletService.getBalance(userId);
        assertEquals(new BigDecimal("75000"), balance.availableBalance());
        assertEquals(new BigDecimal("25000"), balance.heldBalance());
        assertEquals(new BigDecimal("25000"), holdRecord.getAmount());
        assertEquals(HoldRecord.HoldStatus.HELD, holdRecord.getStatus());
    }

    @Test
    void holdForAuctionIncreasesExistingActiveHoldForSameUserAndAuction() {
        UUID userId = UUID.randomUUID();
        UUID auctionId = UUID.randomUUID();
        walletService.topUp(userId, new BigDecimal("100000"));
        HoldRecord firstHold = walletService.holdForAuction(userId, auctionId, new BigDecimal("25000"));

        HoldRecord secondHold = walletService.holdForAuction(userId, auctionId, new BigDecimal("15000"));

        WalletBalanceResponse balance = walletService.getBalance(userId);
        assertSame(firstHold, secondHold);
        assertEquals(new BigDecimal("60000"), balance.availableBalance());
        assertEquals(new BigDecimal("40000"), balance.heldBalance());
        assertEquals(new BigDecimal("40000"), secondHold.getAmount());
        assertEquals(HoldRecord.HoldStatus.HELD, secondHold.getStatus());
    }

    @Test
    void releaseForAuctionReleasesFullActiveHoldBackToAvailableBalance() {
        UUID userId = UUID.randomUUID();
        UUID auctionId = UUID.randomUUID();
        walletService.topUp(userId, new BigDecimal("100000"));
        walletService.holdForAuction(userId, auctionId, new BigDecimal("25000"));
        walletService.holdForAuction(userId, auctionId, new BigDecimal("15000"));

        HoldRecord releasedHold = walletService.releaseForAuction(userId, auctionId, new BigDecimal("40000"));

        WalletBalanceResponse balance = walletService.getBalance(userId);
        assertEquals(new BigDecimal("100000"), balance.availableBalance());
        assertEquals(BigDecimal.ZERO, balance.heldBalance());
        assertEquals(new BigDecimal("40000"), releasedHold.getAmount());
        assertEquals(HoldRecord.HoldStatus.RELEASED, releasedHold.getStatus());
    }

    @Test
    void releaseForAuctionThrowsWhenRequestedAmountDiffersFromActiveHoldAmount() {
        UUID userId = UUID.randomUUID();
        UUID auctionId = UUID.randomUUID();
        walletService.topUp(userId, new BigDecimal("100000"));
        walletService.holdForAuction(userId, auctionId, new BigDecimal("40000"));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> walletService.releaseForAuction(userId, auctionId, new BigDecimal("30000"))
        );

        WalletBalanceResponse balance = walletService.getBalance(userId);
        assertEquals("Hold amount mismatch", exception.getMessage());
        assertEquals(new BigDecimal("60000"), balance.availableBalance());
        assertEquals(new BigDecimal("40000"), balance.heldBalance());
    }

    @Test
    void captureForAuctionCapturesFullActiveHoldWithoutReturningAvailableBalance() {
        UUID userId = UUID.randomUUID();
        UUID auctionId = UUID.randomUUID();
        walletService.topUp(userId, new BigDecimal("100000"));
        walletService.holdForAuction(userId, auctionId, new BigDecimal("25000"));
        walletService.holdForAuction(userId, auctionId, new BigDecimal("15000"));

        HoldRecord capturedHold = walletService.captureForAuction(userId, auctionId, new BigDecimal("40000"));

        WalletBalanceResponse balance = walletService.getBalance(userId);
        assertEquals(new BigDecimal("60000"), balance.availableBalance());
        assertEquals(BigDecimal.ZERO, balance.heldBalance());
        assertEquals(new BigDecimal("40000"), capturedHold.getAmount());
        assertEquals(HoldRecord.HoldStatus.CAPTURED, capturedHold.getStatus());
    }

    @Test
    void captureForAuctionThrowsWhenRequestedAmountDiffersFromActiveHoldAmount() {
        UUID userId = UUID.randomUUID();
        UUID auctionId = UUID.randomUUID();
        walletService.topUp(userId, new BigDecimal("100000"));
        walletService.holdForAuction(userId, auctionId, new BigDecimal("40000"));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> walletService.captureForAuction(userId, auctionId, new BigDecimal("30000"))
        );

        WalletBalanceResponse balance = walletService.getBalance(userId);
        assertEquals("Hold amount mismatch", exception.getMessage());
        assertEquals(new BigDecimal("60000"), balance.availableBalance());
        assertEquals(new BigDecimal("40000"), balance.heldBalance());
    }

    @Test
    void holdForAuctionFailsWhenAvailableBalanceIsInsufficient() {
        UUID userId = UUID.randomUUID();
        UUID auctionId = UUID.randomUUID();
        walletService.topUp(userId, new BigDecimal("10000"));

        assertThrows(
                IllegalArgumentException.class,
                () -> walletService.holdForAuction(userId, auctionId, new BigDecimal("25000"))
        );

        WalletBalanceResponse balance = walletService.getBalance(userId);
        assertEquals(new BigDecimal("10000"), balance.availableBalance());
        assertEquals(BigDecimal.ZERO, balance.heldBalance());
    }
}
