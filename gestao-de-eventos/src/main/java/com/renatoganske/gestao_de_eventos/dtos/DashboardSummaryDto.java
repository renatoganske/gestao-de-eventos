package com.renatoganske.gestao_de_eventos.dtos;

import java.io.Serializable;

/**
 * The dashboard's numbers (ADR-0029), counted by the database instead of by the length of
 * three full event searches.
 *
 * @param eventsThisMonth   events dated within the current month (business time zone)
 * @param pendingDeliveries events whose delivery status is PENDING, whatever their date
 * @param hdsNearCapacity   HDs at or above the near-capacity threshold (same rule as /api/hds/near-capacity)
 */
public record DashboardSummaryDto(
        long eventsThisMonth,
        long pendingDeliveries,
        int hdsNearCapacity
) implements Serializable {
}
