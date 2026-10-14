package com.minimarket.domain.port.out;

import com.minimarket.domain.model.User;

public interface AccessTokenPort {

    IssuedAccessToken issue(User user);

    record IssuedAccessToken(String value, long expiresInSeconds) {
    }
}
