package id.ac.ui.cs.advprog.walletservice.service;

import id.ac.ui.cs.advprog.walletservice.model.HoldRecord;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WalletIdempotencyServiceTest {

    @Test
    void nullIdempotencyKeyIsIgnored() {
        WalletIdempotencyService service = new WalletIdempotencyService();
        HoldRecord holdRecord = createHoldRecord();

        Optional<HoldRecord> initialResult = service.getCachedHoldRecord(null);
        service.cacheHoldRecord(null, holdRecord);
        Optional<HoldRecord> resultAfterCacheAttempt = service.getCachedHoldRecord(null);

        assertTrue(initialResult.isEmpty());
        assertTrue(resultAfterCacheAttempt.isEmpty());
    }

    @Test
    void unknownIdempotencyKeyReturnsEmpty() {
        WalletIdempotencyService service = new WalletIdempotencyService();

        Optional<HoldRecord> result = service.getCachedHoldRecord("unknown-key");

        assertTrue(result.isEmpty());
    }

    @Test
    void cachedHoldRecordCanBeRetrievedByKey() {
        WalletIdempotencyService service = new WalletIdempotencyService();
        HoldRecord holdRecord = createHoldRecord();

        service.cacheHoldRecord("hold-key-1", holdRecord);
        Optional<HoldRecord> result = service.getCachedHoldRecord("hold-key-1");

        assertTrue(result.isPresent());
        assertEquals(holdRecord.getHoldId(), result.get().getHoldId());
    }

    private HoldRecord createHoldRecord() {
        return new HoldRecord(UUID.randomUUID(), UUID.randomUUID(), new BigDecimal("30000"));
    }
}
