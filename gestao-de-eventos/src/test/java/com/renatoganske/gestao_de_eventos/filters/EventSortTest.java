package com.renatoganske.gestao_de_eventos.filters;

import com.renatoganske.gestao_de_eventos.exceptions.InvalidRequestParameterException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EventSortTest {

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"  "})
    void parse_withoutAValue_isNewestFirst(String value) {
        assertThat(EventSort.parse(value)).isEqualTo(new EventSort(EventSort.Key.DATE, false));
    }

    @Test
    void parse_readsEveryColumnKeyAndDirection_ignoringCaseAndSpaces() {
        assertThat(EventSort.parse("code,asc")).isEqualTo(new EventSort(EventSort.Key.CODE, true));
        assertThat(EventSort.parse("Customer,DESC")).isEqualTo(new EventSort(EventSort.Key.CUSTOMER, false));
        assertThat(EventSort.parse(" venues , desc ")).isEqualTo(new EventSort(EventSort.Key.VENUES, false));
        assertThat(EventSort.parse("type,asc")).isEqualTo(new EventSort(EventSort.Key.TYPE, true));
        assertThat(EventSort.parse("date,asc")).isEqualTo(new EventSort(EventSort.Key.DATE, true));
        assertThat(EventSort.parse("hd,desc")).isEqualTo(new EventSort(EventSort.Key.HD, false));
        assertThat(EventSort.parse("status,asc")).isEqualTo(new EventSort(EventSort.Key.STATUS, true));
    }

    @Test
    void parse_withoutDirection_isAscending() {
        assertThat(EventSort.parse("code")).isEqualTo(new EventSort(EventSort.Key.CODE, true));
    }

    @ParameterizedTest
    // property paths are not accepted, only the list's column keys
    @ValueSource(strings = {"eventDate,asc", "name", "date,up", "date,asc,code", ",asc"})
    void parse_rejectsAnythingButAColumnKeyAndADirection(String value) {
        assertThatThrownBy(() -> EventSort.parse(value))
                .isInstanceOf(InvalidRequestParameterException.class)
                .hasMessageContaining(value)
                .hasMessageContaining("code, customer, venues, type, date, hd, status");
    }
}
