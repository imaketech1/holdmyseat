package xyz.imaketech.holdmyseat.booking.catalog;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;
import xyz.imaketech.holdmyseat.booking.config.BookingProperties;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Component
public class CatalogClient {

    public record CatalogShow(UUID id, Instant startsAt, Map<String, BigDecimal> seatPrices) {
    }

    private final RestClient rest;

    public CatalogClient(RestClient.Builder builder, BookingProperties props) {
        this.rest = builder.baseUrl(props.catalogUri()).build();
    }

    public CatalogShow getShow(UUID showId) {
        try {
            return rest.get().uri("/internal/shows/{id}", showId).retrieve().body(CatalogShow.class);
        } catch (HttpClientErrorException.NotFound e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Show not found");
        }
    }
}
