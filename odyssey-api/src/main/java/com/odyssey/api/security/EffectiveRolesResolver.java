package com.odyssey.api.security;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

final class EffectiveRolesResolver {

    private static final String AGENT_ROLE = "AGENT";
    private static final String TRAVELER_ROLE = "TRAVELER";

    private EffectiveRolesResolver() {
    }

    static List<String> resolve(List<String> claimedRoles) {
        Set<String> effectiveRoles = new LinkedHashSet<>();

        if (claimedRoles != null) {
            effectiveRoles.addAll(claimedRoles);
        }

        if (!effectiveRoles.contains(AGENT_ROLE)
            && !effectiveRoles.contains(TRAVELER_ROLE)) {
            effectiveRoles.add(TRAVELER_ROLE);
        }

        return List.copyOf(new ArrayList<>(effectiveRoles));
    }
}