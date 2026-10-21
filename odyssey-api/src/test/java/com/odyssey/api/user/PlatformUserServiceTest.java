package com.odyssey.api.user;

import com.odyssey.api.agent.Agent;
import com.odyssey.api.agent.AgentRepository;
import com.odyssey.api.agent.AgentStatus;
import com.odyssey.api.traveler.Traveler;
import com.odyssey.api.traveler.TravelerRepository;
import com.odyssey.api.user.PlatformUserResponse.ProfileType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PlatformUserServiceTest {

    @Mock
    private TravelerRepository travelerRepository;
    @Mock
    private AgentRepository agentRepository;
    private PlatformUserService service;

    @BeforeEach
    void setUp() {
        service = new PlatformUserService(travelerRepository, agentRepository);
    }

    @Test
    void mapsTravelerContactDetailsAndPagination() {
        Traveler traveler = new Traveler("Marie", "Martin", "marie@example.com");
        traveler.setId(12L);
        traveler.setPhoneNumber("+33612345678");
        traveler.setWhatsappNumber("+33687654321");
        traveler.setPreferredLanguage("fr");
        traveler.setOnboardingCompleted(true);
        var pageable = PageRequest.of(1, 20, Sort.by("lastName", "firstName", "id"));
        when(travelerRepository.searchProfiles("Marie", pageable))
            .thenReturn(new PageImpl<>(List.of(traveler), pageable, 21));

        var result = service.getUsers(ProfileType.TRAVELER, " Marie ", 1, 20);

        assertEquals(21, result.totalElements());
        assertEquals(2, result.totalPages());
        assertEquals(1, result.page());
        assertEquals(20, result.size());
        var user = result.items().getFirst();
        assertEquals(12L, user.id());
        assertEquals(ProfileType.TRAVELER, user.profileType());
        assertEquals("Marie", user.firstName());
        assertEquals("Martin", user.lastName());
        assertEquals("marie@example.com", user.email());
        assertEquals("+33612345678", user.phoneNumber());
        assertEquals("+33687654321", user.whatsappNumber());
        assertEquals("fr", user.preferredLanguage());
        assertTrue(user.onboardingCompleted());
        assertNull(user.agentStatus());
        verifyNoInteractions(agentRepository);
    }

    @Test
    void mapsAgentWithoutInventingTravelerInformation() {
        Agent agent = new Agent();
        agent.setEmail("agent@example.com");
        agent.setStatus(AgentStatus.BUSY);
        var pageable = PageRequest.of(0, 20, Sort.by("lastName", "firstName", "id"));
        when(agentRepository.searchProfiles("", pageable))
            .thenReturn(new PageImpl<>(List.of(agent), pageable, 1));

        var user = service.getUsers(ProfileType.AGENT, "", 0, 20).items().getFirst();

        assertEquals(ProfileType.AGENT, user.profileType());
        assertEquals("agent@example.com", user.email());
        assertEquals(AgentStatus.BUSY, user.agentStatus());
        assertNull(user.firstName());
        assertNull(user.phoneNumber());
        assertNull(user.whatsappNumber());
        assertNull(user.preferredLanguage());
        assertNull(user.onboardingCompleted());
        verifyNoInteractions(travelerRepository);
    }

    @ParameterizedTest
    @CsvSource({"-1,20", "0,0", "0,101"})
    void rejectsInvalidPagination(int page, int size) {
        assertThrows(IllegalArgumentException.class,
            () -> service.getUsers(ProfileType.TRAVELER, "", page, size));
        verifyNoInteractions(travelerRepository, agentRepository);
    }

    @Test
    void rejectsOverlongSearch() {
        assertThrows(IllegalArgumentException.class,
            () -> service.getUsers(ProfileType.TRAVELER, "a".repeat(121), 0, 20));
        verifyNoInteractions(travelerRepository, agentRepository);
    }
}
