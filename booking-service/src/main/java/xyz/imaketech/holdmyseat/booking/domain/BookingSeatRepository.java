package xyz.imaketech.holdmyseat.booking.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface BookingSeatRepository extends JpaRepository<BookingSeat, UUID> {

    @Query("select s from BookingSeat s where s.showId = :showId and s.status in :statuses")
    List<BookingSeat> findByShowAndStatus(UUID showId, Collection<SeatStatus> statuses);
}
