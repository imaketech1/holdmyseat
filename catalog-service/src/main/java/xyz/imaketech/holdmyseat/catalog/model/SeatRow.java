package xyz.imaketech.holdmyseat.catalog.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;
import java.util.stream.IntStream;

/** One row of a screen, e.g. row "A" with 15 seats in category "PREMIUM" priced at 1.5x. */
public record SeatRow(String row, int seats, String category, BigDecimal priceMultiplier) implements Serializable {

    public List<String> seatIds() {
        return IntStream.rangeClosed(1, seats).mapToObj(n -> row + n).toList();
    }
}
