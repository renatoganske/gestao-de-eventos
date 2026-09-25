package com.renatoganske.gestao_de_eventos.policies;

import com.renatoganske.gestao_de_eventos.entities.Hd;

public final class HdCapacityPolicy {

    private static final double NEAR_CAPACITY_THRESHOLD = 0.9;

    private HdCapacityPolicy() {
    }

    public static boolean isNearCapacity(Hd hd) {
        if (hd.getUsedSpaceGb() == null || hd.getCapacityGb() == null) {
            return false;
        }
        return hd.getUsedSpaceGb() >= hd.getCapacityGb() * NEAR_CAPACITY_THRESHOLD;
    }
}
