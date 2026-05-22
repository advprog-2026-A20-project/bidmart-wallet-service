package id.ac.ui.cs.advprog.walletservice.controller;

final class WalletInternalControllerTestConstants {
    static final String INTERNAL_CAPTURE_ENDPOINT = "/wallet/internal/capture";
    static final String INTERNAL_CREDIT_ENDPOINT = "/wallet/internal/credit";
    static final String INTERNAL_HOLD_ENDPOINT = "/wallet/internal/hold";
    static final String INTERNAL_RELEASE_ENDPOINT = "/wallet/internal/release";
    static final String INTERNAL_TOKEN_HEADER = "X-Internal-Token";
    static final String TEST_INTERNAL_TOKEN = "test-internal-token";
    static final String INTERNAL_SERVICE_TOKEN_PROPERTY = "internal.service-token=" + TEST_INTERNAL_TOKEN;

    private WalletInternalControllerTestConstants() {
    }
}
