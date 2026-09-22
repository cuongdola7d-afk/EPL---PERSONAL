package com.premierhub.config;

import com.premierhub.csv.ClubCsvReader;
import com.premierhub.repository.ClubRepository;
import com.premierhub.repository.InMemoryClubRepository;
import com.premierhub.service.ClubService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;

@Configuration
public class ClubDataConfiguration {
    @Bean
    public ClubRepository clubRepository() throws IOException {
        var resource = new ClassPathResource("data/clubs.csv");
        var clubs = new ClubCsvReader().read(resource.getInputStream());
        return new InMemoryClubRepository(clubs);
    }

    @Bean
    public ClubService clubService(ClubRepository repository) {
        return new ClubService(repository);
    }
}
