package id.ac.ui.cs.advprog.walletservice.service;

import id.ac.ui.cs.advprog.walletservice.model.HoldRecord;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

@Service
public class WalletIdempotencyService {
    private final Map<String, HoldRecord> holdRecordCache = new ConcurrentHashMap<>();

    public Optional<HoldRecord> getCachedHoldRecord(String idempotencyKey) {
        if (idempotencyKey == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(holdRecordCache.get(idempotencyKey));
    }

    public void cacheHoldRecord(String idempotencyKey, HoldRecord holdRecord) {
        if (idempotencyKey == null) {
            return;
        }
        holdRecordCache.put(idempotencyKey, holdRecord);
    }
}
