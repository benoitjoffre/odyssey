package com.odyssey.api.security;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static org.springframework.util.StringUtils.hasText;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import com.odyssey.api.agent.Agent;
import com.odyssey.api.agent.AgentRepository;
import com.odyssey.api.agent.AgentStatus;
import com.odyssey.api.traveler.Traveler;
import com.odyssey.api.traveler.TravelerRepository;

@Service
public class CurrentUserService {

    private static final Logger logger =
        LoggerFactory.getLogger(CurrentUserService.class);

    private final AgentRepository agentRepository;
    private final TravelerRepository travelerRepository;

    public CurrentUserService(
        AgentRepository agentRepository,
        TravelerRepository travelerRepository
    ) {
        this.agentRepository = agentRepository;
        this.travelerRepository = travelerRepository;
    }

    public void provisionUser(Jwt jwt) {
        String auth0Subject = jwt.getSubject();
        String email = jwt.getClaimAsString("https://odyssey.app/email");
        List<String> roles = jwt.getClaimAsStringList("https://odyssey.app/roles");
        String firstName = jwt.getClaimAsString("https://odyssey.app/given_name");
        String lastName = jwt.getClaimAsString("https://odyssey.app/family_name");

        if (roles == null) {
            return;
        }

        if (roles.contains("AGENT")) {
            provisionAgent(auth0Subject, email, firstName, lastName);
        }

        if (roles.contains("TRAVELER")) {
            provisionTraveler(auth0Subject, email, firstName, lastName);
        }
    }

    private void provisionAgent(String auth0Subject, String email, String firstName, String lastName) {
        if (!hasText(auth0Subject) || !hasText(email)) {
            logger.warn("Cannot provision AGENT: JWT subject or email claim is missing");
            return;
        }

        if (agentRepository.findByAuth0Subject(auth0Subject).isPresent()) {
            return;
        }

        var existingAgent = agentRepository.findByEmail(email);
        if (existingAgent.isPresent()) {
            Agent agent = existingAgent.get();
            if (hasText(agent.getAuth0Subject())
                && !auth0Subject.equals(agent.getAuth0Subject())) {
                logger.warn(
                    "Cannot link AGENT email {}: it is already linked to another Auth0 subject",
                    email
                );
                return;
            }
            agent.setAuth0Subject(auth0Subject);
            agentRepository.save(agent);
            return;
        }

        Agent agent = new Agent();
        agent.setAuth0Subject(auth0Subject);
        agent.setEmail(email);
        agent.setStatus(AgentStatus.AVAILABLE);

        agentRepository.save(agent);
    }

    private void provisionTraveler(
        String auth0Subject,
        String email,
        String firstName,
        String lastName
    ) {
        if (!hasText(auth0Subject) || !hasText(email)) {
            logger.warn("Cannot provision TRAVELER: JWT subject or email claim is missing");
            return;
        }

        if (travelerRepository.findByAuth0Subject(auth0Subject).isPresent()) {
            return;
        }

        var existingTraveler = travelerRepository.findByEmail(email);
        if (existingTraveler.isPresent()) {
            Traveler traveler = existingTraveler.get();
            if (hasText(traveler.getAuth0Subject())
                && !auth0Subject.equals(traveler.getAuth0Subject())) {
                logger.warn(
                    "Cannot link TRAVELER email {}: it is already linked to another Auth0 subject",
                    email
                );
                return;
            }
            traveler.setAuth0Subject(auth0Subject);
            travelerRepository.save(traveler);
            return;
        }

        Traveler traveler = new Traveler();
        traveler.setAuth0Subject(auth0Subject);
        traveler.setEmail(email);
        if (hasText(firstName)) {
            traveler.setFirstName(firstName);
        }
        if (hasText(lastName)) {
            traveler.setLastName(lastName);
        }

        travelerRepository.save(traveler);
    }

}