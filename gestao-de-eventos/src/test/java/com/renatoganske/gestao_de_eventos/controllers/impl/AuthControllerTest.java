package com.renatoganske.gestao_de_eventos.controllers.impl;

import com.renatoganske.gestao_de_eventos.configs.security.JwtService;
import com.renatoganske.gestao_de_eventos.configs.security.LoginRateLimiter;
import com.renatoganske.gestao_de_eventos.dtos.LoginRequestDto;
import com.renatoganske.gestao_de_eventos.dtos.LoginResponseDto;
import com.renatoganske.gestao_de_eventos.exceptions.TooManyLoginAttemptsException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtService jwtService;

    @Mock
    private LoginRateLimiter loginRateLimiter;

    @InjectMocks
    private AuthController authController;

    @Test
    void login_withValidCredentials_returnsToken() {
        LoginRequestDto requestDto = new LoginRequestDto("admin", "correct-password");
        Authentication authentication = new UsernamePasswordAuthenticationToken("admin", null);
        when(authenticationManager.authenticate(any())).thenReturn(authentication);
        when(jwtService.generateToken("admin")).thenReturn("signed-jwt");

        ResponseEntity<LoginResponseDto> response = authController.login(requestDto);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody().token()).isEqualTo("signed-jwt");
        verify(loginRateLimiter).onSuccess("admin");
    }

    @Test
    void login_withInvalidCredentials_propagatesAuthenticationExceptionAndRecordsFailure() {
        LoginRequestDto requestDto = new LoginRequestDto("admin", "wrong-password");
        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("bad credentials"));

        assertThatThrownBy(() -> authController.login(requestDto))
                .isInstanceOf(BadCredentialsException.class);

        verify(loginRateLimiter).onFailure("admin");
    }

    @Test
    void login_whenRateLimited_throwsWithoutCallingAuthenticationManager() {
        LoginRequestDto requestDto = new LoginRequestDto("admin", "correct-password");
        when(loginRateLimiter.isBlocked("admin")).thenReturn(true);

        assertThatThrownBy(() -> authController.login(requestDto))
                .isInstanceOf(TooManyLoginAttemptsException.class);

        verify(authenticationManager, never()).authenticate(any());
    }
}
