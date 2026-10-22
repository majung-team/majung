package app.majung.server.pickup;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

interface PickupRepository extends JpaRepository<Pickup, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Pickup p where p.id = :id")
    Optional<Pickup> findLockedById(UUID id);
}
