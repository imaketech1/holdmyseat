package xyz.imaketech.holdmyseat.catalog.model;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ScreenRepository extends JpaRepository<Screen, UUID> {
}
