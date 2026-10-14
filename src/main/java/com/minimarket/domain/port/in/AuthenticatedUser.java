package com.minimarket.domain.port.in;

import com.minimarket.domain.model.UserRole;

public record AuthenticatedUser(Long id, String username, String fullName, UserRole role) {
}
