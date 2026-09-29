package com.renatoganske.gestao_de_eventos.policies;

import com.renatoganske.gestao_de_eventos.entities.Hd;

public final class HdCapacityPolicy {

    private static final double NEAR_CAPACITY_THRESHOLD = 0.9;

    private HdCapacityPolicy() {
    }

    public static boolean isNearCapacity(Hd hd) {
        Integer effectiveCapacityGb = hd.getRealCapacityGb() != null ? hd.getRealCapacityGb() : hd.getCapacityGb();
        if (hd.getUsedSpaceGb() == null || effectiveCapacityGb == null) {
            return false;
        }
        return hd.getUsedSpaceGb() >= effectiveCapacityGb * NEAR_CAPACITY_THRESHOLD;
    }
}
