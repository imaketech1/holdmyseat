package xyz.imaketech.holdmyseat.catalog.model;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "shows")
public class Show {

    @Id
    private UUID id = UUID.randomUUID();

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "screen_id")
    private Screen screen;

    private String title;
    private String language;

    @Column(name = "starts_at")
    private Instant startsAt;

    @Column(name = "base_price")
    private BigDecimal basePrice;

    protected Show() {
    }

    public Show(Screen screen, String title, String language, Instant startsAt, BigDecimal basePrice) {
        this.screen = screen;
        this.title = title;
        this.language = language;
        this.startsAt = startsAt;
        this.basePrice = basePrice;
    }

    public UUID getId() { return id; }
    public Screen getScreen() { return screen; }
    public String getTitle() { return title; }
    public String getLanguage() { return language; }
    public Instant getStartsAt() { return startsAt; }
    public BigDecimal getBasePrice() { return basePrice; }
}
