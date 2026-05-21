package id.ac.ui.cs.advprog.walletservice.controller;

import id.ac.ui.cs.advprog.walletservice.dto.WalletBalanceResponse;
import id.ac.ui.cs.advprog.walletservice.repository.HoldRepository;
import id.ac.ui.cs.advprog.walletservice.repository.TransactionRepository;
import id.ac.ui.cs.advprog.walletservice.repository.WalletRepository;
import id.ac.ui.cs.advprog.walletservice.service.WalletService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class WalletInternalControllerSuccessTest {
    private WalletService walletService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        walletService = new WalletService(
                new WalletRepository(),
                new HoldRepository(),
                new TransactionRepository()
        );
        mockMvc = MockMvcBuilders
                .standaloneSetup(new WalletInternalController(walletService))
                .build();
    }

    @Test
    void internalHoldReturnsNoContentAndMovesAvailableBalanceToHeldBalance() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID auctionId = UUID.randomUUID();
        walletService.topUp(userId, new BigDecimal("100000"));

        mockMvc.perform(post("/wallet/internal/hold")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(internalFundsRequest(userId, auctionId, "25000")))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        WalletBalanceResponse balance = walletService.getBalance(userId);
        assertEquals(new BigDecimal("75000"), balance.availableBalance());
        assertEquals(new BigDecimal("25000"), balance.heldBalance());
    }

    @Test
    void internalReleaseReturnsNoContentAndRestoresHeldBalanceToAvailableBalance() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID auctionId = UUID.randomUUID();
        walletService.topUp(userId, new BigDecimal("100000"));
        walletService.holdForAuction(userId, auctionId, new BigDecimal("25000"));

        mockMvc.perform(post("/wallet/internal/release")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(internalFundsRequest(userId, auctionId, "25000")))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        WalletBalanceResponse balance = walletService.getBalance(userId);
        assertEquals(new BigDecimal("100000"), balance.availableBalance());
        assertEquals(BigDecimal.ZERO, balance.heldBalance());
    }

    @Test
    void internalCaptureReturnsNoContentAndDoesNotRestoreCapturedBalance() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID auctionId = UUID.randomUUID();
        walletService.topUp(userId, new BigDecimal("100000"));
        walletService.holdForAuction(userId, auctionId, new BigDecimal("25000"));

        mockMvc.perform(post("/wallet/internal/capture")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(internalFundsRequest(userId, auctionId, "25000")))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        WalletBalanceResponse balance = walletService.getBalance(userId);
        assertEquals(new BigDecimal("75000"), balance.availableBalance());
        assertEquals(BigDecimal.ZERO, balance.heldBalance());
    }

    @Test
    void internalCreditReturnsNoContentAndAddsSellerAvailableBalance() throws Exception {
        UUID sellerId = UUID.randomUUID();
        UUID auctionId = UUID.randomUUID();

        mockMvc.perform(post("/wallet/internal/credit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(internalFundsRequest(sellerId, auctionId, "25000")))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        WalletBalanceResponse balance = walletService.getBalance(sellerId);
        assertEquals(new BigDecimal("25000"), balance.availableBalance());
        assertEquals(BigDecimal.ZERO, balance.heldBalance());
    }

    private String internalFundsRequest(UUID userId, UUID auctionId, String amount) {
        return """
                {
                  "userId": "%s",
                  "auctionId": "%s",
                  "amount": %s
                }
                """.formatted(userId, auctionId, amount);
    }
}
