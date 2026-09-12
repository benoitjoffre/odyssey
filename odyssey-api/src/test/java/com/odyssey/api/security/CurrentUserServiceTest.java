package com.odyssey.api.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.Jwt;

import com.odyssey.api.agent.Agent;
import com.odyssey.api.agent.AgentRepository;
import com.odyssey.api.agent.AgentStatus;
import com.odyssey.api.traveler.Traveler;
import com.odyssey.api.traveler.TravelerRepository;

@ExtendWith(MockitoExtension.class)
class CurrentUserServiceTest {

    private static final String ROLES_CLAIM = "https://odyssey.app/roles";
    private static final String EMAIL_CLAIM = "https://odyssey.app/email";
    private static final String FIRST_NAME_CLAIM = "https://odyssey.app/given_name";
    private static final String LAST_NAME_CLAIM = "https://odyssey.app/family_name";

    @Mock
    private AgentRepository agentRepository;

    @Mock
    private TravelerRepository travelerRepository;

    private CurrentUserService currentUserService;

    @BeforeEach
    void setUp() {
        currentUserService = new CurrentUserService(
            agentRepository,
            travelerRepository
        );
    }

    @Test
    void provisionsNewTravelerFromAuth0Claims() {
        Jwt jwt = jwt(
            "auth0|traveler-1",
            "traveler@example.com",
            List.of("TRAVELER"),
            "Alice",
            "Martin"
        );
        when(travelerRepository.findByAuth0Subject("auth0|traveler-1"))
            .thenReturn(Optional.empty());
        when(travelerRepository.findByEmail("traveler@example.com"))
            .thenReturn(Optional.empty());

        currentUserService.provisionUser(jwt);

        ArgumentCaptor<Traveler> captor = ArgumentCaptor.forClass(Traveler.class);
        verify(travelerRepository).save(captor.capture());
        Traveler traveler = captor.getValue();
        assertEquals("auth0|traveler-1", traveler.getAuth0Subject());
        assertEquals("traveler@example.com", traveler.getEmail());
        assertEquals("Alice", traveler.getFirstName());
        assertEquals("Martin", traveler.getLastName());
        verify(agentRepository, never()).save(any());
    }

    @Test
    void repeatedTravelerProvisioningDoesNotCreateDuplicate() {
        Jwt jwt = jwt(
            "auth0|traveler-1",
            "traveler@example.com",
            List.of("TRAVELER"),
            null,
            null
        );
        Traveler existingTraveler = new Traveler(
            null,
            null,
            "traveler@example.com"
        );
        existingTraveler.setAuth0Subject("auth0|traveler-1");
        when(travelerRepository.findByAuth0Subject("auth0|traveler-1"))
            .thenReturn(Optional.of(existingTraveler));

        currentUserService.provisionUser(jwt);

        verify(travelerRepository, never()).findByEmail(any());
        verify(travelerRepository, never()).save(any());
    }

    @Test
    void linksExistingTravelerByEmailInsteadOfCreatingDuplicate() {
        Jwt jwt = jwt(
            "auth0|traveler-1",
            "traveler@example.com",
            List.of("TRAVELER"),
            null,
            null
        );
        Traveler existingTraveler = new Traveler(
            "Alice",
            "Martin",
            "traveler@example.com"
        );
        when(travelerRepository.findByAuth0Subject("auth0|traveler-1"))
            .thenReturn(Optional.empty());
        when(travelerRepository.findByEmail("traveler@example.com"))
            .thenReturn(Optional.of(existingTraveler));

        currentUserService.provisionUser(jwt);

        verify(travelerRepository).save(existingTraveler);
        assertEquals("auth0|traveler-1", existingTraveler.getAuth0Subject());
    }

    @Test
    void doesNotReassignTravelerLinkedToAnotherAuth0Subject() {
        Jwt jwt = jwt(
            "auth0|traveler-2",
            "traveler@example.com",
            List.of("TRAVELER"),
            "Alice",
            "Martin"
        );
        Traveler existingTraveler = new Traveler(
            "Alice",
            "Martin",
            "traveler@example.com"
        );
        existingTraveler.setAuth0Subject("auth0|traveler-1");
        when(travelerRepository.findByAuth0Subject("auth0|traveler-2"))
            .thenReturn(Optional.empty());
        when(travelerRepository.findByEmail("traveler@example.com"))
            .thenReturn(Optional.of(existingTraveler));

        currentUserService.provisionUser(jwt);

        verify(travelerRepository, never()).save(any());
        assertEquals("auth0|traveler-1", existingTraveler.getAuth0Subject());
    }

    @Test
    void provisionsTravelerWithoutInventingNamesWhenClaimsAreMissing() {
        Jwt jwt = jwt(
            "auth0|traveler-1",
            "traveler@example.com",
            List.of("TRAVELER"),
            null,
            null
        );
        when(travelerRepository.findByAuth0Subject("auth0|traveler-1"))
            .thenReturn(Optional.empty());
        when(travelerRepository.findByEmail("traveler@example.com"))
            .thenReturn(Optional.empty());

        currentUserService.provisionUser(jwt);

        ArgumentCaptor<Traveler> captor = ArgumentCaptor.forClass(Traveler.class);
        verify(travelerRepository).save(captor.capture());
        Traveler traveler = captor.getValue();
        assertEquals("auth0|traveler-1", traveler.getAuth0Subject());
        assertEquals("traveler@example.com", traveler.getEmail());
        assertNull(traveler.getFirstName());
        assertNull(traveler.getLastName());
    }

    @Test
    void preservesAgentProvisioning() {
        Jwt jwt = jwt(
            "auth0|agent-1",
            "agent@example.com",
            List.of("AGENT"),
            null,
            null
        );
        when(agentRepository.findByAuth0Subject("auth0|agent-1"))
            .thenReturn(Optional.empty());
        when(agentRepository.findByEmail("agent@example.com"))
            .thenReturn(Optional.empty());

        currentUserService.provisionUser(jwt);

        ArgumentCaptor<Agent> captor = ArgumentCaptor.forClass(Agent.class);
        verify(agentRepository).save(captor.capture());
        Agent agent = captor.getValue();
        assertEquals("auth0|agent-1", agent.getAuth0Subject());
        assertEquals("agent@example.com", agent.getEmail());
        assertEquals(AgentStatus.AVAILABLE, agent.getStatus());
        assertNull(agent.getFirstName());
        assertNull(agent.getLastName());
        verify(travelerRepository, never()).save(any());
    }

    private Jwt jwt(
        String subject,
        String email,
        List<String> roles,
        String firstName,
        String lastName
    ) {
        Jwt.Builder builder = Jwt.withTokenValue("token")
            .header("alg", "none")
            .subject(subject)
            .claim(EMAIL_CLAIM, email)
            .claim(ROLES_CLAIM, roles);

        if (firstName != null) {
            builder.claim(FIRST_NAME_CLAIM, firstName);
        }
        if (lastName != null) {
            builder.claim(LAST_NAME_CLAIM, lastName);
        }

        return builder.build();
    }
}