package id.ac.ui.cs.advprog.walletservice.controller;

import id.ac.ui.cs.advprog.walletservice.dto.WalletBalanceResponse;
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

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
    private static final String INTERNAL_CAPTURE_ENDPOINT = "/wallet/internal/capture";
    private static final String INTERNAL_CREDIT_ENDPOINT = "/wallet/internal/credit";
    private static final String INTERNAL_HOLD_ENDPOINT = "/wallet/internal/hold";
    private static final String INTERNAL_RELEASE_ENDPOINT = "/wallet/internal/release";
    private static final String INTERNAL_TOKEN_HEADER = "X-Internal-Token";

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

        mockMvc.perform(post(INTERNAL_HOLD_ENDPOINT)
                        .header(INTERNAL_TOKEN_HEADER, WalletInternalControllerTestConstants.TEST_INTERNAL_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(internalFundsRequest(userId, auctionId, AMOUNT_25000)))
                .andExpect(status().isNoContent())
                .andExpect(content().string(EMPTY_RESPONSE_BODY));

        WalletBalanceResponse balance = walletService.getBalance(userId);
        assertMoney(AMOUNT_75000, balance.availableBalance());
        assertMoney(AMOUNT_25000, balance.heldBalance());
    }

    @Test
    void internalReleaseReturnsNoContentAndRestoresHeldBalanceToAvailableBalance() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID auctionId = UUID.randomUUID();
        walletService.topUp(userId, new BigDecimal(AMOUNT_100000));
        walletService.holdForAuction(userId, auctionId, new BigDecimal(AMOUNT_25000));

        mockMvc.perform(post(INTERNAL_RELEASE_ENDPOINT)
                        .header(INTERNAL_TOKEN_HEADER, WalletInternalControllerTestConstants.TEST_INTERNAL_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(internalFundsRequest(userId, auctionId, AMOUNT_25000)))
                .andExpect(status().isNoContent())
                .andExpect(content().string(EMPTY_RESPONSE_BODY));

        WalletBalanceResponse balance = walletService.getBalance(userId);
        assertMoney(AMOUNT_100000, balance.availableBalance());
        assertMoney(AMOUNT_0, balance.heldBalance());
    }

    @Test
    void internalCaptureReturnsNoContentAndDoesNotRestoreCapturedBalance() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID auctionId = UUID.randomUUID();
        walletService.topUp(userId, new BigDecimal(AMOUNT_100000));
        walletService.holdForAuction(userId, auctionId, new BigDecimal(AMOUNT_25000));

        mockMvc.perform(post(INTERNAL_CAPTURE_ENDPOINT)
                        .header(INTERNAL_TOKEN_HEADER, WalletInternalControllerTestConstants.TEST_INTERNAL_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(internalFundsRequest(userId, auctionId, AMOUNT_25000)))
                .andExpect(status().isNoContent())
                .andExpect(content().string(EMPTY_RESPONSE_BODY));

        WalletBalanceResponse balance = walletService.getBalance(userId);
        assertMoney(AMOUNT_75000, balance.availableBalance());
        assertMoney(AMOUNT_0, balance.heldBalance());
    }

    @Test
    void internalCreditReturnsNoContentAndAddsSellerAvailableBalance() throws Exception {
        UUID sellerId = UUID.randomUUID();
        UUID auctionId = UUID.randomUUID();

        mockMvc.perform(post(INTERNAL_CREDIT_ENDPOINT)
                        .header(INTERNAL_TOKEN_HEADER, WalletInternalControllerTestConstants.TEST_INTERNAL_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(internalFundsRequest(sellerId, auctionId, AMOUNT_25000)))
                .andExpect(status().isNoContent())
                .andExpect(content().string(EMPTY_RESPONSE_BODY));

        WalletBalanceResponse balance = walletService.getBalance(sellerId);
        assertMoney(AMOUNT_25000, balance.availableBalance());
        assertMoney(AMOUNT_0, balance.heldBalance());
    }

    private void assertMoney(String expected, BigDecimal actual) {
        assertEquals(0, actual.compareTo(new BigDecimal(expected)));
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
