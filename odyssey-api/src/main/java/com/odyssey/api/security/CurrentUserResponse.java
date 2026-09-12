package com.odyssey.api.security;
import java.util.List;
public record CurrentUserResponse(
    List<String> roles
) {}
