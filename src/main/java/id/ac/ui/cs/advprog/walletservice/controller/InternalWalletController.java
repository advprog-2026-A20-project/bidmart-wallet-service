package id.ac.ui.cs.advprog.walletservice.controller;

import id.ac.ui.cs.advprog.walletservice.dto.WalletCommandRequest;
import id.ac.ui.cs.advprog.walletservice.service.WalletGateway;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/internal/wallet")
public class InternalWalletController {

    private final WalletGateway walletGateway;
    private final String internalToken;

    public InternalWalletController(
        WalletGateway walletGateway,
        @org.springframework.beans.factory.annotation.Value("${internal.service-token:}")
        String internalToken
    ) {
        this.walletGateway = walletGateway;
        this.internalToken = internalToken;
    }

    @PostMapping("/hold")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void hold(
        @RequestHeader(value = "X-Internal-Token", required = false) String token,
        @Valid @RequestBody WalletCommandRequest request
    ) {
        ensureInternalToken(token);
        walletGateway.holdFunds(request.userId(), request.auctionId(), request.amount());
    }

    @PostMapping("/release")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void release(
        @RequestHeader(value = "X-Internal-Token", required = false) String token,
        @Valid @RequestBody WalletCommandRequest request
    ) {
        ensureInternalToken(token);
        walletGateway.releaseFunds(request.userId(), request.auctionId(), request.amount());
    }

    @PostMapping("/capture")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void capture(
        @RequestHeader(value = "X-Internal-Token", required = false) String token,
        @Valid @RequestBody WalletCommandRequest request
    ) {
        ensureInternalToken(token);
        walletGateway.captureFunds(request.userId(), request.auctionId(), request.amount());
    }

    @PostMapping("/credit")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void credit(
        @RequestHeader(value = "X-Internal-Token", required = false) String token,
        @Valid @RequestBody WalletCommandRequest request
    ) {
        ensureInternalToken(token);
        walletGateway.creditFunds(request.userId(), request.auctionId(), request.amount());
    }

    private void ensureInternalToken(String token) {
        if (internalToken == null || internalToken.isBlank()) {
            return;
        }
        if (!internalToken.equals(token)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Invalid internal service token");
        }
    }
}
