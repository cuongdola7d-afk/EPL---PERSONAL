package com.premierhub.web.dto;

import com.premierhub.model.Club;

public record ClubResponse(int id, String name, String city, String managerName, String managerStatus,
                           String stadiumName, java.time.LocalDate informationVerifiedOn) {
    public ClubResponse(int id, String name, String city) {
        this(id, name, city, null, null, null, null);
    }
    public static ClubResponse from(Club club) {
        return new ClubResponse(club.getId(), club.getName(), club.getCity());
    }
}
