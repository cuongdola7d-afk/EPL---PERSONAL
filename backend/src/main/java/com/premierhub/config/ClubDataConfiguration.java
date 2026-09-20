package com.premierhub.config;

import com.premierhub.csv.ClubCsvReader;
import com.premierhub.service.PremierHubService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.util.List;

@Configuration
public class ClubDataConfiguration {
    @Bean
    public PremierHubService premierHubService() throws IOException {
        var resource = new ClassPathResource("data/clubs.csv");
        var clubs = new ClubCsvReader().read(resource.getInputStream());
        return new PremierHubService(clubs, List.of(), List.of());
    }
}
