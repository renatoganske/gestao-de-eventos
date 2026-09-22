package com.renatoganske.gestao_de_eventos.enums;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HdStatusTest {

    @Test
    void hasExpectedConstants() {
        assertThat(HdStatus.values())
                .extracting(Enum::name)
                .containsExactlyInAnyOrder("ACTIVE", "FULL", "DEFECTIVE", "ARCHIVED");
    }
}
