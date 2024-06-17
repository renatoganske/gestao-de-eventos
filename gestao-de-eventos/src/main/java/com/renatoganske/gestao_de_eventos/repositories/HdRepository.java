package com.renatoganske.gestao_de_eventos.repositories;

import com.renatoganske.gestao_de_eventos.entities.Hd;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface HdRepository extends JpaRepository<Hd, UUID> {
}