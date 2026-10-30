package xyz.imaketech.holdmyseat.booking.outbox;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface OutboxRepository extends JpaRepository<OutboxEvent, UUID> {

    @Query(value = """
            select * from outbox where published_at is null
            order by created_at limit :limit
            for update skip locked""", nativeQuery = true)
    List<OutboxEvent> lockUnpublished(int limit);
}
