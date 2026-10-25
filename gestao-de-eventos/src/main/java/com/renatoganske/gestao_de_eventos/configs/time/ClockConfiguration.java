package com.renatoganske.gestao_de_eventos.configs.time;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

/**
 * The clock "today" is read from (dashboard month, upcoming events). Pinned to the business time
 * zone instead of the server's: Render runs in UTC, so from 21:00 in Brazil the server's date is
 * already tomorrow. Injected, so tests can fix the date.
 */
@Configuration
public class ClockConfiguration {

    @Bean
    public Clock clock(@Value("${app.time-zone}") String timeZone) {
        return Clock.system(ZoneId.of(timeZone));
    }
}
