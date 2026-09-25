package com.renatoganske.gestao_de_eventos.entities;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity(name = "TB_APP_USER")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class AppUser {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "username", nullable = false, unique = true)
    private String username;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;
}
