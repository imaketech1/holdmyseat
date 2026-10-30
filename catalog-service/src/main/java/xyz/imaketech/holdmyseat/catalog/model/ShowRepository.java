package xyz.imaketech.holdmyseat.catalog.model;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ShowRepository extends JpaRepository<Show, UUID> {

    @Query("""
            select s from Show s join fetch s.screen sc join fetch sc.venue v
            where lower(v.city) = lower(:city) and s.startsAt > :after
            order by s.startsAt""")
    List<Show> findUpcomingInCity(String city, Instant after);

    @Query("""
            select s from Show s join fetch s.screen sc join fetch sc.venue v
            where lower(v.city) = lower(:city) and s.startsAt >= :from and s.startsAt < :to
            order by s.startsAt""")
    List<Show> findInCityBetween(String city, Instant from, Instant to);

    @Query("select s from Show s where s.screen.id = :screenId and s.title = :title and s.startsAt >= :from and s.startsAt < :to")
    List<Show> findOnScreenBetween(UUID screenId, String title, Instant from, Instant to);

    @Query("select s from Show s join fetch s.screen sc join fetch sc.venue where s.id = :id")
    Optional<Show> findWithScreen(UUID id);

    @Modifying
    @Query("update Show s set s.startsAt = :startsAt where s.id = :id")
    int updateStartsAt(UUID id, Instant startsAt);
}
