package com.odyssey.api.destination;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DestinationRepository extends JpaRepository<Destination, Long> {

    Optional<Destination> findByCityIgnoreCaseAndCountryCodeIgnoreCase(
        String city,
        String countryCode
    );

    List<Destination> findAllByOrderByCountryAscCityAsc();
}
