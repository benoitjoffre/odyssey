package com.odyssey.api.security;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class CurrentUserController {

    private final CurrentUserService currentUserService;

    public CurrentUserController(CurrentUserService currentUserService) {
        this.currentUserService = currentUserService;
    } 

    @GetMapping("/me")
    public CurrentUserResponse getCurrentUser(
        @AuthenticationPrincipal Jwt jwt
    ) {
        currentUserService.provisionUser(jwt);
        return new CurrentUserResponse(
            jwt.getClaimAsStringList("https://odyssey.app/roles")
        );
    }
}
