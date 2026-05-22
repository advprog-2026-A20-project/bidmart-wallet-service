package id.ac.ui.cs.advprog.walletservice.controller;

import id.ac.ui.cs.advprog.walletservice.repository.HoldRepository;
import id.ac.ui.cs.advprog.walletservice.repository.TransactionRepository;
import id.ac.ui.cs.advprog.walletservice.repository.WalletRepository;
import id.ac.ui.cs.advprog.walletservice.service.WalletService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static id.ac.ui.cs.advprog.walletservice.controller.WalletInternalControllerTestConstants.INTERNAL_CAPTURE_ENDPOINT;
import static id.ac.ui.cs.advprog.walletservice.controller.WalletInternalControllerTestConstants.INTERNAL_HOLD_ENDPOINT;
import static id.ac.ui.cs.advprog.walletservice.controller.WalletInternalControllerTestConstants.INTERNAL_RELEASE_ENDPOINT;
import static id.ac.ui.cs.advprog.walletservice.controller.WalletInternalControllerTestSupport.performInternalPost;
import static id.ac.ui.cs.advprog.walletservice.controller.WalletInternalControllerTestSupport.performInternalPostWithoutToken;
import static id.ac.ui.cs.advprog.walletservice.controller.WalletInternalControllerTestSupport.topUpAndHoldForAuction;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = WalletInternalControllerTestConstants.INTERNAL_SERVICE_TOKEN_PROPERTY)
@AutoConfigureMockMvc
class WalletInternalControllerExceptionHandlerTest {
    private static final String AMOUNT_0 = "0";
    private static final String AMOUNT_25000 = "25000";
    private static final String AMOUNT_30000 = "30000";
    private static final String AMOUNT_40000 = "40000";
    private static final String AMOUNT_100000 = "100000";

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
        performInternalPost(mockMvc, INTERNAL_HOLD_ENDPOINT, UUID.randomUUID(), UUID.randomUUID(), AMOUNT_25000)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code", is("INSUFFICIENT_BALANCE")))
                .andExpect(jsonPath("$.message", is("Insufficient balance")));
    }

    @Test
    void internalEndpointWithoutTokenReturnsUnauthorized() throws Exception {
        performInternalPostWithoutToken(
                mockMvc,
                INTERNAL_HOLD_ENDPOINT,
                UUID.randomUUID(),
                UUID.randomUUID(),
                AMOUNT_25000
        )
                .andExpect(status().isUnauthorized());
    }

    @Test
    void internalHoldWithInvalidAmountReturnsBadRequestError() throws Exception {
        performInternalPost(mockMvc, INTERNAL_HOLD_ENDPOINT, UUID.randomUUID(), UUID.randomUUID(), AMOUNT_0)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("INVALID_AMOUNT")))
                .andExpect(jsonPath("$.message", is("Amount must be greater than zero")));
    }

    @Test
    void internalReleaseWithoutActiveHoldReturnsNotFoundError() throws Exception {
        performInternalPost(mockMvc, INTERNAL_RELEASE_ENDPOINT, UUID.randomUUID(), UUID.randomUUID(), AMOUNT_25000)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code", is("ACTIVE_HOLD_NOT_FOUND")))
                .andExpect(jsonPath("$.message", is("Active hold not found for auction")));
    }

    @Test
    void internalReleaseWithMismatchedAmountReturnsConflictError() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID auctionId = UUID.randomUUID();
        topUpAndHoldForAuction(walletService, userId, auctionId, AMOUNT_100000, AMOUNT_40000);

        performInternalPost(mockMvc, INTERNAL_RELEASE_ENDPOINT, userId, auctionId, AMOUNT_30000)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code", is("HOLD_AMOUNT_MISMATCH")))
                .andExpect(jsonPath("$.message", is("Hold amount mismatch")));
    }

    @Test
    void internalCaptureWithMismatchedAmountReturnsConflictError() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID auctionId = UUID.randomUUID();
        topUpAndHoldForAuction(walletService, userId, auctionId, AMOUNT_100000, AMOUNT_40000);

        performInternalPost(mockMvc, INTERNAL_CAPTURE_ENDPOINT, userId, auctionId, AMOUNT_30000)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code", is("HOLD_AMOUNT_MISMATCH")))
                .andExpect(jsonPath("$.message", is("Hold amount mismatch")));
    }
}
