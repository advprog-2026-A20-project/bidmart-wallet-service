package id.ac.ui.cs.advprog.walletservice.repository;

import id.ac.ui.cs.advprog.walletservice.model.HoldRecord;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class HoldRepository {
    private final Map<UUID, HoldRecord> holds = new ConcurrentHashMap<>();

    public HoldRecord save(HoldRecord holdRecord) {
        holds.put(holdRecord.getHoldId(), holdRecord);
        return holdRecord;
    }

    public Optional<HoldRecord> findById(UUID holdId) {
        return Optional.ofNullable(holds.get(holdId));
    }

    public Optional<HoldRecord> findActiveByUserIdAndAuctionId(UUID userId, UUID auctionId) {
        return holds.values().stream()
                .filter(holdRecord -> holdRecord.getStatus() == HoldRecord.HoldStatus.HELD)
                .filter(holdRecord -> holdRecord.getUserId().equals(userId))
                .filter(holdRecord -> auctionId.equals(holdRecord.getAuctionId()))
                .findFirst();
    }
}
