package com.premierhub.admin;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
    private final AdminOverviewService overviewService;

    public AdminController(AdminOverviewService overviewService) {
        this.overviewService = overviewService;
    }

    @GetMapping("/overview")
    public ResponseEntity<?> overview(@RequestParam(name = "range", defaultValue = "30") int range) {
        if (range != 7 && range != 30) {
            return ResponseEntity.badRequest()
                    .header(HttpHeaders.CACHE_CONTROL, "no-store")
                    .body(Map.of("code", "INVALID_RANGE", "message", "Tham số range chỉ hỗ trợ 7 hoặc 30."));
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(overviewService.overview(range));
    }
}
