package xyz.imaketech.holdmyseat.catalog.web;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import xyz.imaketech.holdmyseat.catalog.model.*;
import xyz.imaketech.holdmyseat.catalog.web.Dtos.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Cached in Redis; seat availability is owned by Booking Service. */
@Service
@Transactional(readOnly = true)
public class CatalogService {

    private static final Duration MAX_RANGE = Duration.ofDays(8);

    private final VenueRepository venues;
    private final ScreenRepository screens;
    private final ShowRepository shows;

    public CatalogService(VenueRepository venues, ScreenRepository screens, ShowRepository shows) {
        this.venues = venues;
        this.screens = screens;
        this.shows = shows;
    }

    @Cacheable(cacheNames = "shows-by-city", key = "#city.toLowerCase()")
    public List<ShowSummary> upcomingShows(String city) {
        return shows.findUpcomingInCity(city, Instant.now()).stream().map(CatalogService::summary).toList();
    }

    /** Includes shows that already started. */
    @Cacheable(cacheNames = "shows-by-city", key = "#city.toLowerCase() + ':' + #from + ':' + #to")
    public List<ShowSummary> showsBetween(String city, Instant from, Instant to) {
        if (!from.isBefore(to) || Duration.between(from, to).compareTo(MAX_RANGE) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "from must be before to, at most 8 days apart");
        }
        return shows.findInCityBetween(city, from, to).stream().map(CatalogService::summary).toList();
    }

    private static ShowSummary summary(Show s) {
        return new ShowSummary(s.getId(), s.getTitle(), s.getLanguage(), s.getStartsAt(),
                s.getBasePrice(), s.getScreen().getVenue().getName(), s.getScreen().getVenue().getCity());
    }

    @Cacheable(cacheNames = "show-detail", key = "#id")
    public ShowDetail show(UUID id) {
        Show s = load(id);
        return new ShowDetail(s.getId(), s.getTitle(), s.getLanguage(), s.getStartsAt(), s.getBasePrice(),
                s.getScreen().getVenue().getName(), s.getScreen().getName(), List.copyOf(s.getScreen().getLayout()));
    }

    @Cacheable(cacheNames = "show-internal", key = "#id")
    public InternalShow internalShow(UUID id) {
        Show s = load(id);
        Map<String, BigDecimal> prices = new LinkedHashMap<>();
        for (SeatRow row : s.getScreen().getLayout()) {
            BigDecimal price = s.getBasePrice().multiply(row.priceMultiplier()).setScale(2, RoundingMode.HALF_UP);
            row.seatIds().forEach(seat -> prices.put(seat, price));
        }
        return new InternalShow(s.getId(), s.getStartsAt(), prices);
    }

    @Transactional
    public UUID createVenue(CreateVenue req) {
        return venues.save(new Venue(req.name(), req.city())).getId();
    }

    @Transactional
    public UUID createScreen(CreateScreen req) {
        Venue venue = venues.findById(req.venueId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Venue not found"));
        List<SeatRow> layout = req.layout().stream()
                .map(r -> new SeatRow(r.row(), r.seats(), r.category(), r.priceMultiplier()))
                .toList();
        return screens.save(new Screen(venue, req.name(), layout)).getId();
    }

    @Transactional
    @CacheEvict(cacheNames = "shows-by-city", allEntries = true)
    public UUID createShow(CreateShow req) {
        Screen screen = screens.findById(req.screenId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Screen not found"));
        return shows.save(new Show(screen, req.title(), req.language(), req.startsAt(), req.basePrice())).getId();
    }

    private Show load(UUID id) {
        return shows.findWithScreen(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Show not found"));
    }
}
