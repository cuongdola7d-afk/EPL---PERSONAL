package com.premierhub.config;

import com.premierhub.csv.PlayerCsvReader;
import com.premierhub.repository.InMemoryPlayerRepository;
import com.premierhub.repository.ClubRepository;
import com.premierhub.repository.PlayerRepository;
import com.premierhub.service.PlayerService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import java.io.IOException;

@Configuration
public class PlayerDataConfiguration {
    @Bean
    public PlayerRepository playerRepository() throws IOException {
        var resource = new ClassPathResource("data/players.csv");
        return new InMemoryPlayerRepository(new PlayerCsvReader().read(resource.getInputStream()));
    }

    @Bean
    public PlayerService playerService(PlayerRepository repository, ClubRepository clubs) {
        return new PlayerService(repository, clubs.findAll());
    }
}
