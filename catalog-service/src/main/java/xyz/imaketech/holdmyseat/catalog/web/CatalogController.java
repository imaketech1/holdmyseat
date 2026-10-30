package xyz.imaketech.holdmyseat.catalog.web;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import xyz.imaketech.holdmyseat.catalog.web.Dtos.*;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@RestController
public class CatalogController {

    private final CatalogService catalog;

    public CatalogController(CatalogService catalog) {
        this.catalog = catalog;
    }

    /** Upcoming shows by default; with from/to, every show in that window (started ones included). */
    @GetMapping("/api/catalog/shows")
    public List<ShowSummary> shows(@RequestParam String city,
                                   @RequestParam(required = false) Instant from,
                                   @RequestParam(required = false) Instant to) {
        if (from == null && to == null) return catalog.upcomingShows(city);
        if (from == null || to == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Pass both from and to, or neither");
        }
        return catalog.showsBetween(city, from, to);
    }

    @GetMapping("/api/catalog/shows/{id}")
    public ShowDetail show(@PathVariable UUID id) {
        return catalog.show(id);
    }

    /** Service-to-service only; not routed by the gateway. */
    @GetMapping("/internal/shows/{id}")
    public InternalShow internalShow(@PathVariable UUID id) {
        return catalog.internalShow(id);
    }

    // Admin: also enforced at the gateway

    @PostMapping("/api/admin/catalog/venues")
    @ResponseStatus(HttpStatus.CREATED)
    public IdResponse createVenue(@RequestHeader("X-User-Roles") String roles, @Valid @RequestBody CreateVenue req) {
        requireAdmin(roles);
        return new IdResponse(catalog.createVenue(req));
    }

    @PostMapping("/api/admin/catalog/screens")
    @ResponseStatus(HttpStatus.CREATED)
    public IdResponse createScreen(@RequestHeader("X-User-Roles") String roles, @Valid @RequestBody CreateScreen req) {
        requireAdmin(roles);
        return new IdResponse(catalog.createScreen(req));
    }

    @PostMapping("/api/admin/catalog/shows")
    @ResponseStatus(HttpStatus.CREATED)
    public IdResponse createShow(@RequestHeader("X-User-Roles") String roles, @Valid @RequestBody CreateShow req) {
        requireAdmin(roles);
        return new IdResponse(catalog.createShow(req));
    }

    private void requireAdmin(String roles) {
        if (Arrays.stream(roles.split(",")).noneMatch("ADMIN"::equals)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin only");
        }
    }
}
