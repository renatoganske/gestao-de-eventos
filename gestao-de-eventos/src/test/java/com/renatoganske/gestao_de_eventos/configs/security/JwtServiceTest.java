package com.renatoganske.gestao_de_eventos.configs.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private static final String SECRET = "unit-test-secret-com-pelo-menos-32-bytes";

    private final JwtService jwtService = new JwtService(SECRET, 60_000L);

    @Test
    void generateToken_thenExtractUsername_roundtrips() {
        String token = jwtService.generateToken("renato");

        assertThat(jwtService.extractUsername(token)).isEqualTo("renato");
    }

    @Test
    void isTokenValid_withMatchingUsernameAndUnexpiredToken_returnsTrue() {
        String token = jwtService.generateToken("renato");
        UserDetails userDetails = User.withUsername("renato").password("hash").authorities("USER").build();

        assertThat(jwtService.isTokenValid(token, userDetails)).isTrue();
    }

    @Test
    void isTokenValid_withDifferentUsername_returnsFalse() {
        String token = jwtService.generateToken("renato");
        UserDetails userDetails = User.withUsername("someone-else").password("hash").authorities("USER").build();

        assertThat(jwtService.isTokenValid(token, userDetails)).isFalse();
    }

    @Test
    void isTokenValid_withExpiredToken_returnsFalse() {
        JwtService expiredTokenIssuer = new JwtService(SECRET, -60_000L);
        String token = expiredTokenIssuer.generateToken("renato");
        UserDetails userDetails = User.withUsername("renato").password("hash").authorities("USER").build();

        assertThat(jwtService.isTokenValid(token, userDetails)).isFalse();
    }

    @Test
    void isTokenValid_withMalformedToken_returnsFalseInsteadOfThrowing() {
        UserDetails userDetails = User.withUsername("renato").password("hash").authorities("USER").build();

        assertThat(jwtService.isTokenValid("not-a-jwt", userDetails)).isFalse();
    }
}
