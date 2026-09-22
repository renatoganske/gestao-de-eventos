package com.renatoganske.gestao_de_eventos.enums;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EventTypeTest {

    @Test
    void hasExpectedConstants() {
        assertThat(EventType.values())
                .extracting(Enum::name)
                .containsExactlyInAnyOrder("PHOTO_SHOOT", "BIRTHDAY", "WEDDING", "OTHER");
    }
}
