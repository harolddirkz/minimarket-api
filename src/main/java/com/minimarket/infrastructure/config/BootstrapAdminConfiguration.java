package com.minimarket.infrastructure.config;

import com.minimarket.domain.model.User;
import com.minimarket.domain.model.UserRole;
import com.minimarket.domain.port.out.PasswordHashPort;
import com.minimarket.domain.port.out.BootstrapLockPort;
import com.minimarket.domain.port.out.UserRepositoryPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.Instant;
import java.util.regex.Pattern;

@Configuration
public class BootstrapAdminConfiguration {

    private static final Logger log = LoggerFactory.getLogger(BootstrapAdminConfiguration.class);
    private static final Pattern STRONG_PASSWORD = Pattern.compile(
            "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{12,128}$");

    @Bean
    ApplicationRunner bootstrapAdminRunner(
            SecurityProperties properties,
            UserRepositoryPort userRepository,
            BootstrapLockPort bootstrapLockPort,
            PasswordHashPort passwordHashPort,
            TransactionTemplate transactionTemplate,
            Clock clock) {
        return new ApplicationRunner() {
            @Override
            public void run(ApplicationArguments arguments) {
                SecurityProperties.BootstrapAdmin bootstrap = properties.bootstrapAdmin();
                if (bootstrap.username() == null || bootstrap.username().isBlank()) {
                    return;
                }
                transactionTemplate.executeWithoutResult(status -> {
                    bootstrapLockPort.acquireAdministratorBootstrapLock();
                    if (userRepository.countAdministrators() > 0) {
                        log.info("Initial administrator bootstrap skipped because an administrator already exists");
                        return;
                    }
                    validate(bootstrap);
                    Instant now = clock.instant();
                    User created = userRepository.save(new User(
                            null,
                            bootstrap.username(),
                            passwordHashPort.encode(bootstrap.password()),
                            bootstrap.fullName(),
                            UserRole.ADMIN,
                            true,
                            now,
                            now));
                    log.info("Initial administrator created. userId={}", created.id());
                });
            }
        };
    }

    private static void validate(SecurityProperties.BootstrapAdmin bootstrap) {
        if (bootstrap.username().length() > 80) {
            throw new IllegalStateException("BOOTSTRAP_ADMIN_USERNAME must contain at most 80 characters");
        }
        if (bootstrap.fullName() == null || bootstrap.fullName().isBlank() || bootstrap.fullName().length() > 160) {
            throw new IllegalStateException("BOOTSTRAP_ADMIN_FULL_NAME must contain between 1 and 160 characters");
        }
        if (bootstrap.password() == null || !STRONG_PASSWORD.matcher(bootstrap.password()).matches()) {
            throw new IllegalStateException(
                    "BOOTSTRAP_ADMIN_PASSWORD must have 12-128 characters including upper, lower, number and symbol");
        }
    }
}
