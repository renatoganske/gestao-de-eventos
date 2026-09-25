package com.renatoganske.gestao_de_eventos.configs.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    private final JwtService jwtService = new JwtService("unit-test-secret-com-pelo-menos-32-bytes", 60_000L);

    @Mock
    private UserDetailsService userDetailsService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    private JwtAuthenticationFilter filter;

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private void setUpFilter() {
        filter = new JwtAuthenticationFilter(jwtService, userDetailsService);
    }

    @Test
    void doFilter_withValidToken_setsAuthenticationInContext() throws Exception {
        setUpFilter();
        UserDetails userDetails = User.withUsername("renato").password("hash").authorities("USER").build();
        when(request.getHeader("Authorization")).thenReturn("Bearer " + jwtService.generateToken("renato"));
        when(userDetailsService.loadUserByUsername("renato")).thenReturn(userDetails);

        filter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getName()).isEqualTo("renato");
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilter_withoutAuthorizationHeader_leavesContextEmptyAndContinuesChain() throws Exception {
        setUpFilter();
        when(request.getHeader("Authorization")).thenReturn(null);

        filter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilter_withMalformedToken_leavesContextEmptyAndContinuesChainWithoutThrowing() throws Exception {
        setUpFilter();
        when(request.getHeader("Authorization")).thenReturn("Bearer not-a-real-jwt");

        filter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }
}
