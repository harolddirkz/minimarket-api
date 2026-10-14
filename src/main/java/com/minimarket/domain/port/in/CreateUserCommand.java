package com.minimarket.domain.port.in;

import com.minimarket.domain.model.UserRole;

public record CreateUserCommand(String username, String password, String fullName, UserRole role) {
}
