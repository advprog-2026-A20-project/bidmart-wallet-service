package id.ac.ui.cs.advprog.walletservice.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record WalletResponse(
    UUID id,
    BigDecimal balance,
    BigDecimal availableBalance,
    BigDecimal heldBalance,
    UUID userId
) {}
