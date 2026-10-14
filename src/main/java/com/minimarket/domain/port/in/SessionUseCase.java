package com.minimarket.domain.port.in;

public interface SessionUseCase {

    RefreshSessionResult refresh(RefreshSessionCommand command);

    void logout(LogoutCommand command);
}
