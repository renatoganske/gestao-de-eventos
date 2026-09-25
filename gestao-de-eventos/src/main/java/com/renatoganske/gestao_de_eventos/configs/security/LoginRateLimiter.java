package com.renatoganske.gestao_de_eventos.configs.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Bloqueio simples em memoria por usuario, sem dependencia externa (Bucket4j etc.) --
 * suficiente para uma instancia unica (Render) protegendo uma unica conta admin.
 * Reseta se o processo reiniciar; aceitavel para este escopo (ver ADR-0017).
 */
@Component
public class LoginRateLimiter {

    private record Attempt(int count, Instant windowStart) {
    }

    private final ConcurrentHashMap<String, Attempt> attemptsByUsername = new ConcurrentHashMap<>();
    private final int maxAttempts;
    private final Duration lockoutWindow;

    @Autowired
    public LoginRateLimiter(
            @Value("${app.security.login.max-attempts:5}") int maxAttempts,
            @Value("${app.security.login.lockout-minutes:15}") long lockoutMinutes) {
        this(maxAttempts, Duration.ofMinutes(lockoutMinutes));
    }

    LoginRateLimiter(int maxAttempts, Duration lockoutWindow) {
        this.maxAttempts = maxAttempts;
        this.lockoutWindow = lockoutWindow;
    }

    public boolean isBlocked(String username) {
        String key = key(username);
        Attempt attempt = attemptsByUsername.get(key);
        if (attempt == null) {
            return false;
        }
        if (Instant.now().isAfter(attempt.windowStart().plus(lockoutWindow))) {
            attemptsByUsername.remove(key);
            return false;
        }
        return attempt.count() >= maxAttempts;
    }

    public void onFailure(String username) {
        attemptsByUsername.compute(key(username), (k, current) -> {
            Instant now = Instant.now();
            if (current == null || now.isAfter(current.windowStart().plus(lockoutWindow))) {
                return new Attempt(1, now);
            }
            return new Attempt(current.count() + 1, current.windowStart());
        });
    }

    public void onSuccess(String username) {
        attemptsByUsername.remove(key(username));
    }

    private String key(String username) {
        return username == null ? "" : username.toLowerCase();
    }
}
