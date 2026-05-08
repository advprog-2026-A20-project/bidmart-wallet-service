package id.ac.ui.cs.advprog.walletservice.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record AmountRequest(
        @NotNull @DecimalMin(value = "0.01") BigDecimal amount,
        String idempotencyKey
) {}
