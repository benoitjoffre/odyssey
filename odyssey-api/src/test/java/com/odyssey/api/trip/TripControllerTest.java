package com.odyssey.api.trip;

import com.odyssey.api.exception.GlobalExceptionHandler;
import com.odyssey.api.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TripControllerTest {

    private TripService tripService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        tripService = mock(TripService.class);
        mockMvc = MockMvcBuilders
            .standaloneSetup(new TripController(tripService))
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
    }

    @Test
    void deleteTripReturnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/trips/1"))
            .andExpect(status().isNoContent());

        verify(tripService).deleteTrip(1L);
    }

    @Test
    void deleteTripReturnsNotFoundWhenTripDoesNotExist() throws Exception {
        doThrow(new ResourceNotFoundException("Trip not found"))
            .when(tripService)
            .deleteTrip(1L);

        mockMvc.perform(delete("/api/trips/1"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.message").value("Trip not found"));
    }
}