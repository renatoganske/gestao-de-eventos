package com.renatoganske.gestao_de_eventos.configs.security;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class LoginRateLimiterTest {

    @Test
    void isBlocked_startsFalseForUnknownUsername() {
        LoginRateLimiter limiter = new LoginRateLimiter(3, Duration.ofMinutes(15));

        assertThat(limiter.isBlocked("admin")).isFalse();
    }

    @Test
    void isBlocked_becomesTrueAfterMaxAttemptsReached() {
        LoginRateLimiter limiter = new LoginRateLimiter(3, Duration.ofMinutes(15));

        limiter.onFailure("admin");
        limiter.onFailure("admin");
        assertThat(limiter.isBlocked("admin")).isFalse();

        limiter.onFailure("admin");
        assertThat(limiter.isBlocked("admin")).isTrue();
    }

    @Test
    void isBlocked_isCaseInsensitiveOnUsername() {
        LoginRateLimiter limiter = new LoginRateLimiter(1, Duration.ofMinutes(15));

        limiter.onFailure("Admin");

        assertThat(limiter.isBlocked("admin")).isTrue();
    }

    @Test
    void onSuccess_resetsAttemptCounter() {
        LoginRateLimiter limiter = new LoginRateLimiter(1, Duration.ofMinutes(15));

        limiter.onFailure("admin");
        assertThat(limiter.isBlocked("admin")).isTrue();

        limiter.onSuccess("admin");

        assertThat(limiter.isBlocked("admin")).isFalse();
    }

    @Test
    void isBlocked_expiresAfterLockoutWindowPasses() throws InterruptedException {
        LoginRateLimiter limiter = new LoginRateLimiter(1, Duration.ofMillis(50));

        limiter.onFailure("admin");
        assertThat(limiter.isBlocked("admin")).isTrue();

        Thread.sleep(80);

        assertThat(limiter.isBlocked("admin")).isFalse();
    }

    @Test
    void onFailure_afterWindowExpired_startsANewWindowInsteadOfAccumulating() throws InterruptedException {
        LoginRateLimiter limiter = new LoginRateLimiter(2, Duration.ofMillis(50));

        limiter.onFailure("admin");
        Thread.sleep(80);
        limiter.onFailure("admin");

        assertThat(limiter.isBlocked("admin")).isFalse();
    }

    @Test
    void purgeExpiredEntries_removesEntriesNobodyQueriedAgain() throws InterruptedException {
        // Simula um atacante mandando um username diferente a cada tentativa: cada
        // chave e' escrita uma unica vez e nunca mais consultada, entao a unica forma
        // de nao acumular sem limite e' o expurgo ativo, nao a limpeza sob demanda de
        // isBlocked()/onSuccess().
        LoginRateLimiter limiter = new LoginRateLimiter(5, Duration.ofMillis(50));

        for (int i = 0; i < 100; i++) {
            limiter.onFailure("attacker-username-" + i);
        }
        assertThat(limiter.trackedUsernameCount()).isEqualTo(100);

        Thread.sleep(80);
        limiter.purgeExpiredEntries();

        assertThat(limiter.trackedUsernameCount()).isZero();
    }

    @Test
    void purgeExpiredEntries_keepsEntriesStillWithinTheWindow() {
        LoginRateLimiter limiter = new LoginRateLimiter(5, Duration.ofMinutes(15));

        limiter.onFailure("admin");
        limiter.purgeExpiredEntries();

        assertThat(limiter.trackedUsernameCount()).isEqualTo(1);
    }
}
