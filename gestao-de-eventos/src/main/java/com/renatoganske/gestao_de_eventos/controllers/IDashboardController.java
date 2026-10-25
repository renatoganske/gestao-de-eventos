package com.renatoganske.gestao_de_eventos.controllers;

import com.renatoganske.gestao_de_eventos.dtos.DashboardSummaryDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
@Validated
@Tag(name = "Dashboard Controller", description = "Aggregated numbers for the dashboard.")
public interface IDashboardController {

    @Operation(summary = "Dashboard summary", description = "Counts the events of the current month (business time zone), "
            + "the events with delivery PENDING, and the HDs near capacity (same rule as /api/hds/near-capacity).")
    @GetMapping("/summary")
    ResponseEntity<DashboardSummaryDto> summary();
}
