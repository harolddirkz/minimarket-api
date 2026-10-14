package com.minimarket.domain.port.in;

public interface AuthenticationUseCase {

    LoginResult login(LoginCommand command);
}
