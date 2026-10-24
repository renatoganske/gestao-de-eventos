package com.renatoganske.gestao_de_eventos.services;

import com.renatoganske.gestao_de_eventos.dtos.DashboardSummaryDto;
import com.renatoganske.gestao_de_eventos.enums.DeliveryStatus;
import com.renatoganske.gestao_de_eventos.filters.EventSpecifications;
import com.renatoganske.gestao_de_eventos.repositories.EventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.YearMonth;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final EventRepository eventRepository;
    private final HdService hdService;
    private final Clock clock;

    /** Each number is a COUNT with the same criteria the event search uses (ADR-0029). */
    @Transactional(readOnly = true)
    public DashboardSummaryDto getSummary() {
        YearMonth thisMonth = YearMonth.now(clock);
        return new DashboardSummaryDto(
                eventRepository.count(EventSpecifications.byPeriod(thisMonth.atDay(1), thisMonth.atEndOfMonth())),
                eventRepository.count(EventSpecifications.byDeliveryStatus(DeliveryStatus.PENDING)),
                hdService.countHdsNearCapacity());
    }
}
