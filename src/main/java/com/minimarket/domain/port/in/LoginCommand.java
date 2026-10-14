package com.minimarket.domain.port.in;

public record LoginCommand(String username, String password, String origin) {
}
