package id.ac.ui.cs.advprog.walletservice.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record WalletInternalFundsRequest(
        @NotNull UUID userId,
        @NotNull UUID auctionId,
        @NotNull @DecimalMin(value = "0.01") BigDecimal amount
) {}
