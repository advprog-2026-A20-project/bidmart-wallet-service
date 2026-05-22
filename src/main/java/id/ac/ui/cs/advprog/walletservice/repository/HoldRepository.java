package id.ac.ui.cs.advprog.walletservice.repository;

import id.ac.ui.cs.advprog.walletservice.model.HoldRecord;
import id.ac.ui.cs.advprog.walletservice.model.HoldRecord.HoldStatus;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

public interface HoldRepository extends JpaRepository<HoldRecord, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<HoldRecord> findByHoldId(UUID holdId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<HoldRecord> findFirstByUserIdAndAuctionIdAndStatus(UUID userId, UUID auctionId, HoldStatus status);
}
