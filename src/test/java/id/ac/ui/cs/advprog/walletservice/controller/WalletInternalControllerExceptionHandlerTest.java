package id.ac.ui.cs.advprog.walletservice.controller;

import id.ac.ui.cs.advprog.walletservice.exception.WalletExceptionHandler;
import id.ac.ui.cs.advprog.walletservice.repository.HoldRepository;
import id.ac.ui.cs.advprog.walletservice.repository.TransactionRepository;
import id.ac.ui.cs.advprog.walletservice.repository.WalletRepository;
import id.ac.ui.cs.advprog.walletservice.service.WalletService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.UUID;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class WalletInternalControllerExceptionHandlerTest {
    private MockMvc mockMvc;
    private WalletService walletService;

    @BeforeEach
    void setUp() {
        walletService = new WalletService(
                new WalletRepository(),
                new HoldRepository(),
                new TransactionRepository()
        );
        mockMvc = MockMvcBuilders
                .standaloneSetup(new WalletInternalController(walletService))
                .setControllerAdvice(new WalletExceptionHandler())
                .build();
    }

    @Test
    void internalHoldWithInsufficientBalanceReturnsConflictError() throws Exception {
        mockMvc.perform(post("/wallet/internal/hold")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(internalFundsRequest(UUID.randomUUID(), UUID.randomUUID(), "25000")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code", is("INSUFFICIENT_BALANCE")))
                .andExpect(jsonPath("$.message", is("Insufficient balance")));
    }

    @Test
    void internalHoldWithInvalidAmountReturnsBadRequestError() throws Exception {
        mockMvc.perform(post("/wallet/internal/hold")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(internalFundsRequest(UUID.randomUUID(), UUID.randomUUID(), "0")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("INVALID_AMOUNT")))
                .andExpect(jsonPath("$.message", is("Amount must be greater than zero")));
    }

    @Test
    void internalReleaseWithoutActiveHoldReturnsNotFoundError() throws Exception {
        mockMvc.perform(post("/wallet/internal/release")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(internalFundsRequest(UUID.randomUUID(), UUID.randomUUID(), "25000")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code", is("ACTIVE_HOLD_NOT_FOUND")))
                .andExpect(jsonPath("$.message", is("Active hold not found for auction")));
    }

    @Test
    void internalReleaseWithMismatchedAmountReturnsConflictError() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID auctionId = UUID.randomUUID();
        walletService.topUp(userId, new java.math.BigDecimal("100000"));
        walletService.holdForAuction(userId, auctionId, new java.math.BigDecimal("40000"));

        mockMvc.perform(post("/wallet/internal/release")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(internalFundsRequest(userId, auctionId, "30000")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code", is("HOLD_AMOUNT_MISMATCH")))
                .andExpect(jsonPath("$.message", is("Hold amount mismatch")));
    }

    @Test
    void internalCaptureWithMismatchedAmountReturnsConflictError() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID auctionId = UUID.randomUUID();
        walletService.topUp(userId, new java.math.BigDecimal("100000"));
        walletService.holdForAuction(userId, auctionId, new java.math.BigDecimal("40000"));

        mockMvc.perform(post("/wallet/internal/capture")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(internalFundsRequest(userId, auctionId, "30000")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code", is("HOLD_AMOUNT_MISMATCH")))
                .andExpect(jsonPath("$.message", is("Hold amount mismatch")));
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
