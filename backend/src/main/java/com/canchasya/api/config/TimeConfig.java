package com.canchasya.api.config;
import java.time.Clock;
import java.time.ZoneId;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
@Configuration
public class TimeConfig {
    public static final ZoneId ZONA = ZoneId.of("America/Argentina/Buenos_Aires");
    @Bean public Clock appClock() { return Clock.system(ZONA); }
}
