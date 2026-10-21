package com.odyssey.api.user;

import java.util.List;

import com.odyssey.api.agent.AgentRepository;
import com.odyssey.api.traveler.TravelerRepository;
import com.odyssey.api.user.PlatformUserResponse.ProfileType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PlatformUserService {

    private final TravelerRepository travelerRepository;
    private final AgentRepository agentRepository;

    public PlatformUserService(TravelerRepository travelerRepository, AgentRepository agentRepository) {
        this.travelerRepository = travelerRepository;
        this.agentRepository = agentRepository;
    }

    public record UserPage(
        List<PlatformUserResponse> items, int page, int size, long totalElements, int totalPages
    ) {}

    @Transactional(readOnly = true)
    public UserPage getUsers(ProfileType profileType, String search, int page, int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw new IllegalArgumentException("Page must be non-negative and size must be between 1 and 100");
        }
        String normalizedSearch = search.trim();
        if (normalizedSearch.length() > 120) {
            throw new IllegalArgumentException("Search must not exceed 120 characters");
        }
        var pageable = PageRequest.of(page, size, Sort.by("lastName", "firstName", "id"));
        Page<PlatformUserResponse> users = switch (profileType) {
            case TRAVELER -> travelerRepository.searchProfiles(normalizedSearch, pageable)
                .map(PlatformUserResponse::from);
            case AGENT -> agentRepository.searchProfiles(normalizedSearch, pageable)
                .map(PlatformUserResponse::from);
        };
        return new UserPage(users.getContent(), page, size, users.getTotalElements(), users.getTotalPages());
    }
}
