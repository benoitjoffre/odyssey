package com.odyssey.api.travelevent;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.mock.web.MockMultipartFile;
import com.odyssey.api.image.CloudinaryImageService;

@SpringBootTest
@AutoConfigureMockMvc
class TravelEventControllerSecurityHttpTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TravelEventService travelEventService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @MockitoBean
    private CloudinaryImageService cloudinaryImageService;

    @Test
    void getTravelEventReturnsPubliclyWithoutJwt() throws Exception {
        when(travelEventService.getById(4L)).thenReturn(new TravelEventResponse(
            4L,
            "Festival",
            "Paris",
            LocalDate.parse("2026-10-10"),
            LocalDate.parse("2026-10-12"),
            null,
            null,
            2L
        ));

        mockMvc.perform(get("/api/travel-events/4"))
            .andExpect(status().isOk());

        verify(travelEventService).getById(4L);
    }

    @Test
    void getTravelEventsReturnsPubliclyWithoutJwt() throws Exception {
        when(travelEventService.getAll()).thenReturn(List.of());

        mockMvc.perform(get("/api/travel-events"))
            .andExpect(status().isOk());

        verify(travelEventService).getAll();
    }

    @Test
    void creatingTravelEventStillRequiresJwt() throws Exception {
        mockMvc.perform(post("/api/travel-events")
                .contentType("application/json")
                .content("{}"))
            .andExpect(status().isUnauthorized());

        verifyNoInteractions(travelEventService);
    }

    @Test
    void uploadingImageStillRequiresJwt() throws Exception {
        mockMvc.perform(multipart("/api/images/experiences/upload")
                .file(new MockMultipartFile("file", "image.png", "image/png", new byte[] { 1 })))
            .andExpect(status().isUnauthorized());

        verifyNoInteractions(cloudinaryImageService);
    }

    @Test
    void updatingTravelEventStillRequiresJwt() throws Exception {
        mockMvc.perform(put("/api/travel-events/4")
                .contentType("application/json")
                .content("""
                    {
                      "name": "Festival",
                      "location": "Paris",
                      "startDate": "2026-10-10",
                      "endDate": "2026-10-12",
                      "description": null,
                      "imageUrl": null,
                      "experienceId": 2
                    }
                    """))
            .andExpect(status().isUnauthorized());

        verifyNoInteractions(travelEventService);
    }
}
