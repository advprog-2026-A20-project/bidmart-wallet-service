package id.ac.ui.cs.advprog.walletservice.controller;

import id.ac.ui.cs.advprog.walletservice.exception.WalletExceptionHandler;
import id.ac.ui.cs.advprog.walletservice.repository.HoldRepository;
import id.ac.ui.cs.advprog.walletservice.repository.TransactionRepository;
import id.ac.ui.cs.advprog.walletservice.repository.WalletRepository;
import id.ac.ui.cs.advprog.walletservice.service.WalletService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "internal.service-token=test-internal-token")
@AutoConfigureMockMvc
class WalletInternalControllerExceptionHandlerTest {
    private static final String INTERNAL_TOKEN_HEADER = "X-Internal-Token";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private WalletService walletService;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private HoldRepository holdRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @BeforeEach
    void setUp() {
        transactionRepository.deleteAll();
        holdRepository.deleteAll();
        walletRepository.deleteAll();
    }

    @Test
    void internalHoldWithInsufficientBalanceReturnsConflictError() throws Exception {
        mockMvc.perform(post("/wallet/internal/hold")
                        .header(INTERNAL_TOKEN_HEADER, "test-internal-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(internalFundsRequest(UUID.randomUUID(), UUID.randomUUID(), "25000")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code", is("INSUFFICIENT_BALANCE")))
                .andExpect(jsonPath("$.message", is("Insufficient balance")));
    }

    @Test
    void internalEndpointWithoutTokenReturnsUnauthorized() throws Exception {
        mockMvc.perform(post("/wallet/internal/hold")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(internalFundsRequest(UUID.randomUUID(), UUID.randomUUID(), "25000")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void internalHoldWithInvalidAmountReturnsBadRequestError() throws Exception {
        mockMvc.perform(post("/wallet/internal/hold")
                        .header(INTERNAL_TOKEN_HEADER, "test-internal-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(internalFundsRequest(UUID.randomUUID(), UUID.randomUUID(), "0")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("INVALID_AMOUNT")))
                .andExpect(jsonPath("$.message", is("Amount must be greater than zero")));
    }

    @Test
    void internalReleaseWithoutActiveHoldReturnsNotFoundError() throws Exception {
        mockMvc.perform(post("/wallet/internal/release")
                        .header(INTERNAL_TOKEN_HEADER, "test-internal-token")
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
                        .header(INTERNAL_TOKEN_HEADER, "test-internal-token")
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
                        .header(INTERNAL_TOKEN_HEADER, "test-internal-token")
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
