package id.ac.ui.cs.advprog.walletservice.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record InternalFundsRequest(
    UUID userId,
    UUID auctionId,
    BigDecimal amount
) {
}
