package com.premierhub.web;

import com.premierhub.service.FantasyLineupService;
import com.premierhub.web.dto.FantasyLineupRequest;
import com.premierhub.web.dto.FantasyValidationResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import java.util.List;

@RestController
public class FantasyController {
    private final FantasyLineupService service;

    public FantasyController(FantasyLineupService service) {
        this.service = service;
    }

    @PostMapping("/api/fantasy/2026/validate")
    public FantasyValidationResponse validate(@Valid @RequestBody FantasyLineupRequest request) {
        return service.validate(request);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<FantasyValidationResponse> invalidBody() {
        return ResponseEntity.badRequest().body(new FantasyValidationResponse(false, 0,
                List.of(new FantasyValidationResponse.Issue("REQUEST", null, null,
                        "Gửi sơ đồ và picks dạng {mã ô: player_id nguyên}."))));
    }
}
