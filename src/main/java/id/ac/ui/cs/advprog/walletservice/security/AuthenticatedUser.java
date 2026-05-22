package id.ac.ui.cs.advprog.walletservice.security;

import java.util.UUID;

public record AuthenticatedUser(
    UUID id,
    String email,
    String role
) {
}
