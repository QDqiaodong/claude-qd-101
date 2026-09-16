package com.kindergarten.controller;

import com.kindergarten.entity.AidLoan;
import com.kindergarten.service.AidLoanService;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class AidLoanController {

    private final AidLoanService service;

    public AidLoanController(AidLoanService service) {
        this.service = service;
    }

    @GetMapping("/loans")
    public List<AidLoan> list(@RequestParam(required = false) Long classroomId,
                              @RequestParam(required = false) String status) {
        return service.list(classroomId, status);
    }

    @PostMapping("/loans")
    public AidLoan create(@RequestBody AidLoan input) {
        return service.create(input);
    }

    @PostMapping("/loans/{id}/giveback")
    public AidLoan giveBack(
            @PathVariable Long id,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate returnDate) {
        return service.giveBack(id, returnDate);
    }
}
