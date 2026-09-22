package com.renatoganske.gestao_de_eventos.enums;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DeliveryStatusTest {

    @Test
    void hasExpectedConstants() {
        assertThat(DeliveryStatus.values())
                .extracting(Enum::name)
                .containsExactlyInAnyOrder("PENDING", "DELIVERED", "ARCHIVED");
    }
}
