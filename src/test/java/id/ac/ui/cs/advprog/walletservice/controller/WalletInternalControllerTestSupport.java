package id.ac.ui.cs.advprog.walletservice.controller;

import id.ac.ui.cs.advprog.walletservice.dto.WalletBalanceResponse;
import id.ac.ui.cs.advprog.walletservice.service.WalletService;
import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static id.ac.ui.cs.advprog.walletservice.controller.WalletInternalControllerTestConstants.INTERNAL_TOKEN_HEADER;
import static id.ac.ui.cs.advprog.walletservice.controller.WalletInternalControllerTestConstants.TEST_INTERNAL_TOKEN;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

final class WalletInternalControllerTestSupport {
    private WalletInternalControllerTestSupport() {
    }

    static void topUpAndHoldForAuction(
            WalletService walletService,
            UUID userId,
            UUID auctionId,
            String topUpAmount,
            String holdAmount
    ) {
        walletService.topUp(userId, new BigDecimal(topUpAmount));
        walletService.holdForAuction(userId, auctionId, new BigDecimal(holdAmount));
    }

    static ResultActions performInternalPost(
            MockMvc mockMvc,
            String endpoint,
            UUID userId,
            UUID auctionId,
            String amount
    ) throws Exception {
        return mockMvc.perform(post(endpoint)
                .header(INTERNAL_TOKEN_HEADER, TEST_INTERNAL_TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .content(internalFundsRequest(userId, auctionId, amount)));
    }

    static ResultActions performInternalPostWithoutToken(
            MockMvc mockMvc,
            String endpoint,
            UUID userId,
            UUID auctionId,
            String amount
    ) throws Exception {
        return mockMvc.perform(post(endpoint)
                .contentType(MediaType.APPLICATION_JSON)
                .content(internalFundsRequest(userId, auctionId, amount)));
    }

    static void assertBalance(
            WalletService walletService,
            UUID userId,
            String expectedAvailable,
            String expectedHeld
    ) {
        WalletBalanceResponse balance = walletService.getBalance(userId);
        assertMoney(expectedAvailable, balance.availableBalance());
        assertMoney(expectedHeld, balance.heldBalance());
    }

    private static void assertMoney(String expected, BigDecimal actual) {
        assertEquals(0, actual.compareTo(new BigDecimal(expected)));
    }

    private static String internalFundsRequest(UUID userId, UUID auctionId, String amount) {
        return """
                {
                  "userId": "%s",
                  "auctionId": "%s",
                  "amount": %s
                }
                """.formatted(userId, auctionId, amount);
    }
}
