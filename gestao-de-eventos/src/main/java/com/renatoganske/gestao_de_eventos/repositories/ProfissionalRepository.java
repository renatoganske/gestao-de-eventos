package com.renatoganske.gestao_de_eventos.repositories;

import com.renatoganske.gestao_de_eventos.entities.Professional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ProfissionalRepository extends JpaRepository<Professional, UUID> {
}