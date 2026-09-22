package com.premierhub.config;

import com.premierhub.csv.MatchCsvReader;
import com.premierhub.repository.InMemoryMatchRepository;
import com.premierhub.repository.MatchRepository;
import com.premierhub.service.MatchService;
import com.premierhub.service.PremierHubService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import java.io.IOException;

@Configuration
public class MatchDataConfiguration {
    @Bean
    public MatchRepository matchRepository() throws IOException {
        var resource = new ClassPathResource("data/matches.csv");
        return new InMemoryMatchRepository(new MatchCsvReader().read(resource.getInputStream()));
    }

    @Bean
    public MatchService matchService(MatchRepository repository, PremierHubService clubService) {
        return new MatchService(repository, clubService.getClubs());
    }
}
