package id.ac.ui.cs.advprog.walletservice.security;

import id.ac.ui.cs.advprog.walletservice.model.Role;
import java.util.UUID;

public record AuthenticatedUser(
    UUID id,
    String email,
    Role role
) {
}
