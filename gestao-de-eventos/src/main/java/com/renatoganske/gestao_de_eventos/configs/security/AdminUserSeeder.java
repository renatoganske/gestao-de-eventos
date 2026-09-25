package com.renatoganske.gestao_de_eventos.configs.security;

import com.renatoganske.gestao_de_eventos.entities.AppUser;
import com.renatoganske.gestao_de_eventos.repositories.AppUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Semeia o unico usuario da aplicacao no primeiro boot (ver ADR-0017). So roda
 * se a tabela estiver vazia -- nao sobrescreve uma senha ja trocada manualmente
 * via runbook (UPDATE direto no banco), o que preserva a via de recuperacao
 * de acesso escolhida no lugar de um fluxo de "esqueci minha senha" por e-mail.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class AdminUserSeeder implements ApplicationRunner {

    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.security.admin.username}")
    private String adminUsername;

    @Value("${app.security.admin.password:}")
    private String adminPassword;

    @Override
    public void run(ApplicationArguments args) {
        if (appUserRepository.count() > 0) {
            return;
        }
        if (adminPassword == null || adminPassword.isBlank()) {
            log.warn("Nenhum usuario cadastrado e ADMIN_PASSWORD nao foi definida -- "
                    + "login ficara indisponivel ate um usuario ser criado.");
            return;
        }
        appUserRepository.save(AppUser.builder()
                .username(adminUsername)
                .passwordHash(passwordEncoder.encode(adminPassword))
                .build());
        log.info("Usuario inicial '{}' criado.", adminUsername);
    }
}
