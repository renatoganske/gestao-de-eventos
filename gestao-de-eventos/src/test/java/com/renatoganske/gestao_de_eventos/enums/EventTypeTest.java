package com.renatoganske.gestao_de_eventos.enums;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EventTypeTest {

    @Test
    void hasExpectedConstants() {
        assertThat(EventType.values()).containsExactly(
                EventType.PHOTO_SHOOT,
                EventType.BIRTHDAY,
                EventType.WEDDING,
                EventType.OTHER
        );
    }
}
