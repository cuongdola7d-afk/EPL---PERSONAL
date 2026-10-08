package com.premierhub.minigame;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.time.Clock;

@Configuration
@ConditionalOnProperty(name = "premierhub.minigame.enabled", havingValue = "true")
public class PlayerGuessConfiguration {
    @Bean("playerGuessClock")
    Clock playerGuessClock() { return Clock.systemUTC(); }
}
