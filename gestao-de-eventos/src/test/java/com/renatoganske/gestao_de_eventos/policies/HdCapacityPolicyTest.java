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
}
