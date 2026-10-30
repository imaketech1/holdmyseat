package xyz.imaketech.holdmyseat.catalog.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "venues")
public class Venue {

    @Id
    private UUID id = UUID.randomUUID();
    private String name;
    private String city;

    protected Venue() {
    }

    public Venue(String name, String city) {
        this.name = name;
        this.city = city;
    }

    public UUID getId() { return id; }
    public String getName() { return name; }
    public String getCity() { return city; }
}
