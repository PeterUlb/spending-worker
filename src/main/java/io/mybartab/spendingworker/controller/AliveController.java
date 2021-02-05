package io.mybartab.spendingworker.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/alive")
@Slf4j
public class AliveController {

    @GetMapping
    public ResponseEntity<String> alive() {
        return ResponseEntity.ok("ok");
    }
}
