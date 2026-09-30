package com.odyssey.api.destination;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class DestinationServiceTest {

    @Mock
    private DestinationRepository destinationRepository;

    private DestinationService destinationService;

    @BeforeEach
    void setUp() {
        destinationService = new DestinationService(destinationRepository);
    }

    @Test
    void getDestinationsReturnsMappedAndSortedDestinations() {
        Destination first = new Destination();
        ReflectionTestUtils.setField(first, "id", 1L);
        first.setCity("Barcelona");
        first.setCountry("Espagne");
        first.setCountryCode("ES");

        Destination second = new Destination();
        ReflectionTestUtils.setField(second, "id", 2L);
        second.setCity("Bordeaux");
        second.setCountry("France");
        second.setCountryCode("FR");

        when(destinationRepository.findAllByOrderByCountryAscCityAsc())
            .thenReturn(List.of(first, second));

        List<DestinationResponse> responses = destinationService.getDestinations();

        assertEquals(2, responses.size());
        assertEquals(1L, responses.get(0).id());
        assertEquals("Barcelona", responses.get(0).city());
        assertEquals("ES", responses.get(0).countryCode());
        assertEquals(2L, responses.get(1).id());
        assertTrue(responses.stream().allMatch(destination -> destination.id() != null));
    }
}
