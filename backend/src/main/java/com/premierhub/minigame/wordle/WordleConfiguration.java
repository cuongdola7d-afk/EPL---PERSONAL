package com.premierhub.minigame.wordle;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
@ConditionalOnProperty(name = "premierhub.wordle.enabled", havingValue = "true")
public class WordleConfiguration {
    @Bean("wordleClock")
    Clock wordleClock() {
        return Clock.systemUTC();
    }
}
