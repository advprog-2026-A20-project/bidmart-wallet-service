package id.ac.ui.cs.advprog.walletservice.dto;

import java.math.BigDecimal;

public record TopUpRequest(
    BigDecimal amount
) {
}
