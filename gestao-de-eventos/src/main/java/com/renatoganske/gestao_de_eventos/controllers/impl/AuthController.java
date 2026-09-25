package com.renatoganske.gestao_de_eventos.controllers.impl;

import com.renatoganske.gestao_de_eventos.configs.security.JwtService;
import com.renatoganske.gestao_de_eventos.configs.security.LoginRateLimiter;
import com.renatoganske.gestao_de_eventos.controllers.IAuthController;
import com.renatoganske.gestao_de_eventos.dtos.LoginRequestDto;
import com.renatoganske.gestao_de_eventos.dtos.LoginResponseDto;
import com.renatoganske.gestao_de_eventos.exceptions.TooManyLoginAttemptsException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class AuthController implements IAuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final LoginRateLimiter loginRateLimiter;

    @Override
    public ResponseEntity<LoginResponseDto> login(LoginRequestDto requestDto) {
        String username = requestDto.username();
        if (loginRateLimiter.isBlocked(username)) {
            throw new TooManyLoginAttemptsException("Too many failed login attempts. Try again in a few minutes.");
        }

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, requestDto.password()));
            loginRateLimiter.onSuccess(username);

            String token = jwtService.generateToken(authentication.getName());
            return ResponseEntity.ok(new LoginResponseDto(token));
        } catch (AuthenticationException e) {
            loginRateLimiter.onFailure(username);
            throw e;
        }
    }
}
