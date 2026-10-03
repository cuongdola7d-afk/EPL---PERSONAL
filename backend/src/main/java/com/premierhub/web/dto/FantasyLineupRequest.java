package com.premierhub.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Map;

// Only IDs and slot keys are accepted; OVR and permissions are read server-side.
public record FantasyLineupRequest(@NotBlank String formation, @NotNull Map<String, Integer> picks) {
}
