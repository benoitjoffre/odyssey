package com.odyssey.api.security;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

class SecurityConfigTest {

    private static final String ROLES_CLAIM = "https://odyssey.app/roles";

    @Test
    void jwtAuthoritiesDefaultToTravelerWhenRolesClaimIsMissing() {
        SecurityConfig securityConfig = new SecurityConfig();
        JwtAuthenticationToken token = (JwtAuthenticationToken) securityConfig
            .jwtAuthenticationConverter()
            .convert(jwtWithoutRolesClaim());

        Set<String> authorities = token.getAuthorities().stream()
            .map(grantedAuthority -> grantedAuthority.getAuthority())
            .collect(Collectors.toSet());

        assertTrue(authorities.contains("ROLE_TRAVELER"));
        assertTrue(!authorities.contains("ROLE_AGENT"));
    }

    @Test
    void jwtAuthoritiesKeepAgentOnlyWhenAgentIsExplicit() {
        SecurityConfig securityConfig = new SecurityConfig();
        JwtAuthenticationToken token = (JwtAuthenticationToken) securityConfig
            .jwtAuthenticationConverter()
            .convert(jwtWithRoles("AGENT"));

        Set<String> authorities = token.getAuthorities().stream()
            .map(grantedAuthority -> grantedAuthority.getAuthority())
            .collect(Collectors.toSet());

        assertTrue(authorities.contains("ROLE_AGENT"));
        assertTrue(!authorities.contains("ROLE_TRAVELER"));
    }

    @Test
    void jwtAuthoritiesKeepTravelerWhenTravelerIsExplicit() {
        SecurityConfig securityConfig = new SecurityConfig();
        JwtAuthenticationToken token = (JwtAuthenticationToken) securityConfig
            .jwtAuthenticationConverter()
            .convert(jwtWithRoles("TRAVELER"));

        Set<String> authorities = token.getAuthorities().stream()
            .map(grantedAuthority -> grantedAuthority.getAuthority())
            .collect(Collectors.toSet());

        assertTrue(authorities.contains("ROLE_TRAVELER"));
    }

    private Jwt jwtWithoutRolesClaim() {
        return Jwt.withTokenValue("token")
            .header("alg", "none")
            .subject("auth0|subject")
            .build();
    }

    private Jwt jwtWithRoles(String... roles) {
        return Jwt.withTokenValue("token")
            .header("alg", "none")
            .subject("auth0|subject")
            .claim(ROLES_CLAIM, java.util.List.of(roles))
            .build();
    }
}
