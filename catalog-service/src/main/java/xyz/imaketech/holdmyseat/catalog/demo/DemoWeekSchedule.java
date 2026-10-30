package xyz.imaketech.holdmyseat.catalog.demo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import xyz.imaketech.holdmyseat.catalog.model.Screen;
import xyz.imaketech.holdmyseat.catalog.model.ScreenRepository;
import xyz.imaketech.holdmyseat.catalog.model.Show;
import xyz.imaketech.holdmyseat.catalog.model.ShowRepository;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.UUID;

/** Keeps demo shows scheduled daily through Saturday (IST); disable with catalog.demo-shows.weekly=false. */
@Component
@ConditionalOnProperty(name = "catalog.demo-shows.weekly", havingValue = "true", matchIfMissing = true)
public class DemoWeekSchedule {

    private record Movie(String title, String language, LocalTime time, BigDecimal basePrice) {}

    private static final Logger log = LoggerFactory.getLogger(DemoWeekSchedule.class);
    private static final ZoneId IST = ZoneId.of("Asia/Kolkata");
    private static final UUID DEMO_SCREEN = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final List<Movie> MOVIES = List.of(
            new Movie("3 Idiots", "Hindi", LocalTime.of(13, 0), new BigDecimal("200.00")),
            new Movie("Interstellar (Re-release)", "English", LocalTime.of(16, 0), new BigDecimal("200.00")));

    private final ShowRepository shows;
    private final ScreenRepository screens;
    private final CacheManager caches;

    public DemoWeekSchedule(ShowRepository shows, ScreenRepository screens, CacheManager caches) {
        this.shows = shows;
        this.screens = screens;
        this.caches = caches;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Scheduled(cron = "0 0 0 * * *", zone = "Asia/Kolkata")
    @Transactional
    public void fillWeek() {
        Screen screen = screens.findById(DEMO_SCREEN).orElse(null);
        if (screen == null) return;  // demo data removed

        LocalDate today = LocalDate.now(IST);
        LocalDate saturday = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SATURDAY));
        int created = 0;
        for (LocalDate day = today; !day.isAfter(saturday); day = day.plusDays(1)) {
            Instant dayStart = day.atStartOfDay(IST).toInstant();
            Instant dayEnd = day.plusDays(1).atStartOfDay(IST).toInstant();
            for (Movie m : MOVIES) {
                Instant startsAt = day.atTime(m.time()).atZone(IST).toInstant();
                List<Show> existing = shows.findOnScreenBetween(DEMO_SCREEN, m.title(), dayStart, dayEnd);
                if (existing.isEmpty()) {
                    shows.save(new Show(screen, m.title(), m.language(), startsAt, m.basePrice()));
                    created++;
                } else if (!existing.get(0).getStartsAt().equals(startsAt)) {
                    shows.updateStartsAt(existing.get(0).getId(), startsAt);
                }
            }
        }
        for (String name : List.of("shows-by-city", "show-detail", "show-internal")) {
            Cache c = caches.getCache(name);
            if (c != null) c.clear();
        }
        log.info("Demo week {} to {}: {} new shows", today, saturday, created);
    }
}
