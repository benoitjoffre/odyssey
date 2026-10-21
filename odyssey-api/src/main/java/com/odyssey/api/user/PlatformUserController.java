package com.odyssey.api.user;

import com.odyssey.api.user.PlatformUserResponse.ProfileType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@PreAuthorize("hasRole('AGENT')")
public class PlatformUserController {

    private final PlatformUserService platformUserService;

    public PlatformUserController(PlatformUserService platformUserService) {
        this.platformUserService = platformUserService;
    }

    @GetMapping
    public PlatformUserService.UserPage getUsers(
        @RequestParam(defaultValue = "TRAVELER") ProfileType profileType,
        @RequestParam(defaultValue = "") String search,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        return platformUserService.getUsers(profileType, search, page, size);
    }
}
