package com.premierhub.fantasy;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.time.Clock;

@Configuration
public class GameweekConfiguration {
    @Bean("fantasyGameweekClock")
    Clock fantasyGameweekClock() { return Clock.systemUTC(); }
}
