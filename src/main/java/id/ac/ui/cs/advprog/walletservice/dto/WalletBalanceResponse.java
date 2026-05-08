package id.ac.ui.cs.advprog.walletservice.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record WalletBalanceResponse(UUID userId, BigDecimal availableBalance, BigDecimal heldBalance) {}
