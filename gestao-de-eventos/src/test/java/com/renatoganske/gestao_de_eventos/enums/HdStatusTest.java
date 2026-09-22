package com.renatoganske.gestao_de_eventos.enums;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HdStatusTest {

    @Test
    void hasExpectedConstants() {
        assertThat(HdStatus.values()).containsExactly(
                HdStatus.ACTIVE,
                HdStatus.FULL,
                HdStatus.DEFECTIVE,
                HdStatus.ARCHIVED
        );
    }
}
