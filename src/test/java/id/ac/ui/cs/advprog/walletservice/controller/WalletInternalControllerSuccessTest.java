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

@SpringBootTest(properties = "internal.service-token=test-internal-token")
@AutoConfigureMockMvc
class WalletInternalControllerSuccessTest {
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
        walletService.topUp(userId, new BigDecimal("100000"));

        mockMvc.perform(post("/wallet/internal/hold")
                        .header(INTERNAL_TOKEN_HEADER, "test-internal-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(internalFundsRequest(userId, auctionId, "25000")))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        WalletBalanceResponse balance = walletService.getBalance(userId);
        assertMoney("75000", balance.availableBalance());
        assertMoney("25000", balance.heldBalance());
    }

    @Test
    void internalReleaseReturnsNoContentAndRestoresHeldBalanceToAvailableBalance() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID auctionId = UUID.randomUUID();
        walletService.topUp(userId, new BigDecimal("100000"));
        walletService.holdForAuction(userId, auctionId, new BigDecimal("25000"));

        mockMvc.perform(post("/wallet/internal/release")
                        .header(INTERNAL_TOKEN_HEADER, "test-internal-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(internalFundsRequest(userId, auctionId, "25000")))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        WalletBalanceResponse balance = walletService.getBalance(userId);
        assertMoney("100000", balance.availableBalance());
        assertMoney("0", balance.heldBalance());
    }

    @Test
    void internalCaptureReturnsNoContentAndDoesNotRestoreCapturedBalance() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID auctionId = UUID.randomUUID();
        walletService.topUp(userId, new BigDecimal("100000"));
        walletService.holdForAuction(userId, auctionId, new BigDecimal("25000"));

        mockMvc.perform(post("/wallet/internal/capture")
                        .header(INTERNAL_TOKEN_HEADER, "test-internal-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(internalFundsRequest(userId, auctionId, "25000")))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        WalletBalanceResponse balance = walletService.getBalance(userId);
        assertMoney("75000", balance.availableBalance());
        assertMoney("0", balance.heldBalance());
    }

    @Test
    void internalCreditReturnsNoContentAndAddsSellerAvailableBalance() throws Exception {
        UUID sellerId = UUID.randomUUID();
        UUID auctionId = UUID.randomUUID();

        mockMvc.perform(post("/wallet/internal/credit")
                        .header(INTERNAL_TOKEN_HEADER, "test-internal-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(internalFundsRequest(sellerId, auctionId, "25000")))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        WalletBalanceResponse balance = walletService.getBalance(sellerId);
        assertMoney("25000", balance.availableBalance());
        assertMoney("0", balance.heldBalance());
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
