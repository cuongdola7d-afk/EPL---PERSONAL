package com.premierhub.web.dto;

import com.premierhub.model.Club;

public record ClubResponse(int id, String name, String city) {
    public static ClubResponse from(Club club) {
        return new ClubResponse(club.getId(), club.getName(), club.getCity());
    }
}
