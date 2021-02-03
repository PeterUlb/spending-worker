package io.mybartab.spendingworker.controller;

import io.mybartab.spendingworker.service.SpendingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.Optional;

@RestController
public class SpendingController {
    private final SpendingService spendingService;

    public SpendingController(SpendingService spendingService) {
        this.spendingService = spendingService;
    }

    @GetMapping("/{id}")
    public ResponseEntity<Optional<BigDecimal>> getSpendingSum(@PathVariable String id) {
        return ResponseEntity.ok(spendingService.getSumForGroup(id));
    }
}
