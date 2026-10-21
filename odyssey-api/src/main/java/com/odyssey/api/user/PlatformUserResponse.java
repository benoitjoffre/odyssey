package com.odyssey.api.user;

import com.odyssey.api.agent.Agent;
import com.odyssey.api.agent.AgentStatus;
import com.odyssey.api.traveler.Traveler;

public record PlatformUserResponse(
    Long id,
    ProfileType profileType,
    String firstName,
    String lastName,
    String email,
    String phoneNumber,
    String whatsappNumber,
    String preferredLanguage,
    Boolean onboardingCompleted,
    AgentStatus agentStatus
) {
    public enum ProfileType {
        TRAVELER, AGENT
    }

    static PlatformUserResponse from(Traveler traveler) {
        return new PlatformUserResponse(
            traveler.getId(), ProfileType.TRAVELER,
            traveler.getFirstName(), traveler.getLastName(), traveler.getEmail(),
            traveler.getPhoneNumber(), traveler.getWhatsappNumber(), traveler.getPreferredLanguage(),
            traveler.isOnboardingCompleted(), null
        );
    }

    static PlatformUserResponse from(Agent agent) {
        return new PlatformUserResponse(
            agent.getId(), ProfileType.AGENT,
            agent.getFirstName(), agent.getLastName(), agent.getEmail(),
            null, null, null, null, agent.getStatus()
        );
    }
}
