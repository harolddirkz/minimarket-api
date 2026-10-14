package com.minimarket.domain.port.out;

public interface RefreshTokenValuePort {

    String generate();

    String hash(String rawToken);
}
