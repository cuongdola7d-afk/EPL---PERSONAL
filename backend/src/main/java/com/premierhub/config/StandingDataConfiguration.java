package com.premierhub.config;

import com.premierhub.repository.MatchRepository;
import com.premierhub.repository.ClubRepository;
import com.premierhub.service.LeagueTableService;
import com.premierhub.service.StandingService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class StandingDataConfiguration {
    @Bean
    public StandingService standingService(MatchRepository matches, ClubRepository clubs) {
        return new StandingService(new LeagueTableService(), matches, clubs.findAll());
    }
}
