package id.ac.ui.cs.advprog.walletservice.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record HoldResponse(UUID holdId, UUID userId, BigDecimal amount, String status) {}
