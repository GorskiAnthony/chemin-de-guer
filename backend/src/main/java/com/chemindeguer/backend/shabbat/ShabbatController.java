package com.chemindeguer.backend.shabbat;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public/shabbat")
public class ShabbatController {
    private final ShabbatService shabbatService;

    public ShabbatController(ShabbatService shabbatService) {
        this.shabbatService = shabbatService;
    }

    @GetMapping
    public ShabbatStatus status() {
        return shabbatService.status();
    }
}
