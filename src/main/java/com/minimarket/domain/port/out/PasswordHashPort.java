package com.minimarket.domain.port.out;

public interface PasswordHashPort {

    String encode(String rawPassword);

    boolean matches(String rawPassword, String encodedPassword);
}
