package com.odyssey.api.user;

import com.odyssey.api.agent.Agent;
import com.odyssey.api.agent.AgentRepository;
import com.odyssey.api.agent.AgentStatus;
import com.odyssey.api.traveler.Traveler;
import com.odyssey.api.traveler.TravelerRepository;
import com.odyssey.api.user.PlatformUserResponse.ProfileType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class PlatformUserDirectoryIntegrationTest {

    @Autowired
    private TravelerRepository travelerRepository;
    @Autowired
    private AgentRepository agentRepository;
    @Autowired
    private PlatformUserService service;

    @Test
    void searchesPersistedNamesAndEmailsCaseInsensitivelyWithStablePagination() {
        String marker = UUID.randomUUID().toString();
        Traveler first = travelerRepository.save(new Traveler("Alice", marker, marker + "-a@example.com"));
        Traveler second = travelerRepository.save(new Traveler("Bob", marker, marker + "-b@example.com"));

        var firstPage = service.getUsers(ProfileType.TRAVELER, marker.toUpperCase(), 0, 1);
        var secondPage = service.getUsers(ProfileType.TRAVELER, marker, 1, 1);
        assertEquals(2, firstPage.totalElements());
        assertEquals(2, firstPage.totalPages());
        assertEquals(first.getId(), firstPage.items().getFirst().id());
        assertEquals(second.getId(), secondPage.items().getFirst().id());
        assertEquals(1, service.getUsers(ProfileType.TRAVELER, "ALICE " + marker, 0, 20).totalElements());
        assertEquals(1, service.getUsers(ProfileType.TRAVELER, marker + "-b@", 0, 20).totalElements());
        assertTrue(service.getUsers(ProfileType.TRAVELER, marker, 2, 1).items().isEmpty());
    }

    @Test
    void separatesProfilesEvenWhenTheyBelongToTheSamePersonAndHandlesMissingNames() {
        String email = UUID.randomUUID() + "@example.com";
        Traveler traveler = travelerRepository.save(new Traveler(null, null, email));
        Agent agent = new Agent();
        agent.setEmail(email);
        agent.setStatus(AgentStatus.OFFLINE);
        agentRepository.save(agent);

        var travelers = service.getUsers(ProfileType.TRAVELER, email, 0, 20);
        var agents = service.getUsers(ProfileType.AGENT, email, 0, 20);
        assertEquals(1, travelers.totalElements());
        assertEquals(traveler.getId(), travelers.items().getFirst().id());
        assertFalse(travelers.items().getFirst().onboardingCompleted());
        assertNull(travelers.items().getFirst().firstName());
        assertEquals(1, agents.totalElements());
        assertEquals(ProfileType.AGENT, agents.items().getFirst().profileType());
        assertEquals(AgentStatus.OFFLINE, agents.items().getFirst().agentStatus());
        assertNull(agents.items().getFirst().onboardingCompleted());
    }

    @Test
    void treatsSearchWildcardsLiterallyAndListsProfilesWithoutASearch() {
        String marker = UUID.randomUUID().toString();
        Traveler literal = travelerRepository.save(new Traveler("Literal", marker, marker + "%_@example.com"));
        travelerRepository.save(new Traveler("Other", marker, marker + "-other@example.com"));
        Agent agent = new Agent();
        agent.setEmail(marker + "%_@example.com");
        agentRepository.save(agent);

        var travelers = service.getUsers(ProfileType.TRAVELER, marker + "%_", 0, 20);
        assertEquals(1, travelers.totalElements());
        assertEquals(literal.getId(), travelers.items().getFirst().id());
        assertEquals(1, service.getUsers(ProfileType.AGENT, marker + "%_", 0, 20).totalElements());
        assertFalse(service.getUsers(ProfileType.TRAVELER, "", 0, 20).items().isEmpty());
        assertFalse(service.getUsers(ProfileType.AGENT, "", 0, 20).items().isEmpty());
    }
}
