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

import java.math.BigDecimal;
import java.util.UUID;

import static id.ac.ui.cs.advprog.walletservice.controller.WalletInternalControllerTestConstants.INTERNAL_CAPTURE_ENDPOINT;
import static id.ac.ui.cs.advprog.walletservice.controller.WalletInternalControllerTestConstants.INTERNAL_CREDIT_ENDPOINT;
import static id.ac.ui.cs.advprog.walletservice.controller.WalletInternalControllerTestConstants.INTERNAL_HOLD_ENDPOINT;
import static id.ac.ui.cs.advprog.walletservice.controller.WalletInternalControllerTestConstants.INTERNAL_RELEASE_ENDPOINT;
import static id.ac.ui.cs.advprog.walletservice.controller.WalletInternalControllerTestSupport.assertBalance;
import static id.ac.ui.cs.advprog.walletservice.controller.WalletInternalControllerTestSupport.performInternalPost;
import static id.ac.ui.cs.advprog.walletservice.controller.WalletInternalControllerTestSupport.topUpAndHoldForAuction;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = WalletInternalControllerTestConstants.INTERNAL_SERVICE_TOKEN_PROPERTY)
@AutoConfigureMockMvc
class WalletInternalControllerSuccessTest {
    private static final String AMOUNT_0 = "0";
    private static final String AMOUNT_25000 = "25000";
    private static final String AMOUNT_75000 = "75000";
    private static final String AMOUNT_100000 = "100000";
    private static final String EMPTY_RESPONSE_BODY = "";

    @Autowired
    private WalletService walletService;

    @Autowired
    private MockMvc mockMvc;

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
    void internalHoldReturnsNoContentAndMovesAvailableBalanceToHeldBalance() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID auctionId = UUID.randomUUID();
        walletService.topUp(userId, new BigDecimal(AMOUNT_100000));

        performInternalPost(mockMvc, INTERNAL_HOLD_ENDPOINT, userId, auctionId, AMOUNT_25000)
                .andExpect(status().isNoContent())
                .andExpect(content().string(EMPTY_RESPONSE_BODY));

        assertBalance(walletService, userId, AMOUNT_75000, AMOUNT_25000);
    }

    @Test
    void internalReleaseReturnsNoContentAndRestoresHeldBalanceToAvailableBalance() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID auctionId = UUID.randomUUID();
        topUpAndHoldForAuction(walletService, userId, auctionId, AMOUNT_100000, AMOUNT_25000);

        performInternalPost(mockMvc, INTERNAL_RELEASE_ENDPOINT, userId, auctionId, AMOUNT_25000)
                .andExpect(status().isNoContent())
                .andExpect(content().string(EMPTY_RESPONSE_BODY));

        assertBalance(walletService, userId, AMOUNT_100000, AMOUNT_0);
    }

    @Test
    void internalCaptureReturnsNoContentAndDoesNotRestoreCapturedBalance() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID auctionId = UUID.randomUUID();
        topUpAndHoldForAuction(walletService, userId, auctionId, AMOUNT_100000, AMOUNT_25000);

        performInternalPost(mockMvc, INTERNAL_CAPTURE_ENDPOINT, userId, auctionId, AMOUNT_25000)
                .andExpect(status().isNoContent())
                .andExpect(content().string(EMPTY_RESPONSE_BODY));

        assertBalance(walletService, userId, AMOUNT_75000, AMOUNT_0);
    }

    @Test
    void internalCreditReturnsNoContentAndAddsSellerAvailableBalance() throws Exception {
        UUID sellerId = UUID.randomUUID();
        UUID auctionId = UUID.randomUUID();

        performInternalPost(mockMvc, INTERNAL_CREDIT_ENDPOINT, sellerId, auctionId, AMOUNT_25000)
                .andExpect(status().isNoContent())
                .andExpect(content().string(EMPTY_RESPONSE_BODY));

        assertBalance(walletService, sellerId, AMOUNT_25000, AMOUNT_0);
    }
}
