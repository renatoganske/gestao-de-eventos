package com.renatoganske.gestao_de_eventos.policies;

import com.renatoganske.gestao_de_eventos.entities.Hd;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HdCapacityPolicyTest {

    @Test
    void isNearCapacity_trueWhenUsedSpaceAtNinetyPercentThreshold() {
        Hd hd = Hd.builder().capacityGb(1000).usedSpaceGb(900).build();

        assertThat(HdCapacityPolicy.isNearCapacity(hd)).isTrue();
    }

    @Test
    void isNearCapacity_trueWhenUsedSpaceAboveThreshold() {
        Hd hd = Hd.builder().capacityGb(1000).usedSpaceGb(950).build();

        assertThat(HdCapacityPolicy.isNearCapacity(hd)).isTrue();
    }

    @Test
    void isNearCapacity_falseWhenUsedSpaceBelowThreshold() {
        Hd hd = Hd.builder().capacityGb(1000).usedSpaceGb(500).build();

        assertThat(HdCapacityPolicy.isNearCapacity(hd)).isFalse();
    }

    @Test
    void isNearCapacity_falseWhenUsedSpaceIsNull() {
        Hd hd = Hd.builder().capacityGb(1000).usedSpaceGb(null).build();

        assertThat(HdCapacityPolicy.isNearCapacity(hd)).isFalse();
    }

    @Test
    void isNearCapacity_falseWhenCapacityIsNull() {
        Hd hd = Hd.builder().capacityGb(null).usedSpaceGb(900).build();

        assertThat(HdCapacityPolicy.isNearCapacity(hd)).isFalse();
    }

    @Test
    void isNearCapacity_usesRealCapacityWhenPresent() {
        // 1000 GB nominal, 931 GB real: 850 GB used is >= 90% of 931 but < 90% of 1000
        Hd hd = Hd.builder().capacityGb(1000).realCapacityGb(931).usedSpaceGb(850).build();

        assertThat(HdCapacityPolicy.isNearCapacity(hd)).isTrue();
    }

    @Test
    void isNearCapacity_fallsBackToNominalWhenRealCapacityIsNull() {
        Hd hd = Hd.builder().capacityGb(1000).realCapacityGb(null).usedSpaceGb(850).build();

        assertThat(HdCapacityPolicy.isNearCapacity(hd)).isFalse();
    }

    @Test
    void isNearCapacity_usesRealCapacityEvenWhenNominalIsNull() {
        Hd hd = Hd.builder().capacityGb(null).realCapacityGb(931).usedSpaceGb(850).build();

        assertThat(HdCapacityPolicy.isNearCapacity(hd)).isTrue();
    }
}
