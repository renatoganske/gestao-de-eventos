package com.renatoganske.gestao_de_eventos.repositories;

import com.renatoganske.gestao_de_eventos.entities.LocalDoEvento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface LocalDoEventoRepository extends JpaRepository<LocalDoEvento, UUID> {
}