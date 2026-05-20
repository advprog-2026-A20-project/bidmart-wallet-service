package id.ac.ui.cs.advprog.walletservice.controller;

import id.ac.ui.cs.advprog.walletservice.dto.WalletInternalFundsRequest;
import id.ac.ui.cs.advprog.walletservice.service.WalletService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/wallet/internal")
public class WalletInternalController {
    private final WalletService walletService;

    public WalletInternalController(WalletService walletService) {
        this.walletService = walletService;
    }

    @PostMapping("/hold")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void hold(@Valid @RequestBody WalletInternalFundsRequest request) {
        walletService.holdForAuction(request.userId(), request.auctionId(), request.amount());
    }

    @PostMapping("/release")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void release(@Valid @RequestBody WalletInternalFundsRequest request) {
        walletService.releaseForAuction(request.userId(), request.auctionId(), request.amount());
    }

    @PostMapping("/capture")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void capture(@Valid @RequestBody WalletInternalFundsRequest request) {
        walletService.captureForAuction(request.userId(), request.auctionId(), request.amount());
    }
}
