package com.minimarket.application.service;

import com.minimarket.domain.exception.BusinessException;

import java.util.regex.Pattern;

final class PasswordPolicy {

    private static final Pattern STRONG_PASSWORD = Pattern.compile(
            "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{12,128}$");

    private PasswordPolicy() {
    }

    static void validate(String password) {
        if (password == null || !STRONG_PASSWORD.matcher(password).matches()) {
            throw new BusinessException(
                    "WEAK_PASSWORD",
                    "La contraseña debe tener entre 12 y 128 caracteres e incluir mayúscula, minúscula, número y símbolo");
        }
    }
}
