package xyz.imaketech.holdmyseat.catalog.model;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "screens")
public class Screen {

    @Id
    private UUID id = UUID.randomUUID();

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "venue_id")
    private Venue venue;

    private String name;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private List<SeatRow> layout;

    protected Screen() {
    }

    public Screen(Venue venue, String name, List<SeatRow> layout) {
        this.venue = venue;
        this.name = name;
        this.layout = layout;
    }

    public UUID getId() { return id; }
    public Venue getVenue() { return venue; }
    public String getName() { return name; }
    public List<SeatRow> getLayout() { return layout; }
}
