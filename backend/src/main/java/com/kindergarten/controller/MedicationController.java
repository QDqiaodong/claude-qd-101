package com.kindergarten.controller;

import com.kindergarten.entity.MedicationOrder;
import com.kindergarten.service.MedicationService;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class MedicationController {

    private final MedicationService service;

    public MedicationController(MedicationService service) {
        this.service = service;
    }

    @GetMapping("/medications")
    public List<MedicationOrder> list(@RequestParam(required = false) Long classroomId,
                                      @RequestParam(required = false) String status) {
        return service.list(classroomId, status);
    }

    @PostMapping("/medications")
    public MedicationOrder create(@RequestBody MedicationOrder input) {
        return service.create(input);
    }

    @PostMapping("/medications/{id}/execute")
    public MedicationOrder execute(@PathVariable Long id,
                                   @RequestParam(required = false)
                                   @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime actualTime) {
        return service.execute(id, actualTime);
    }

    @PostMapping("/medications/{id}/close")
    public MedicationOrder close(@PathVariable Long id,
                                 @RequestParam(required = false) String reason) {
        return service.close(id, reason);
    }

    @PostMapping("/medications/{id}/withdraw")
    public MedicationOrder withdraw(@PathVariable Long id) {
        return service.withdraw(id);
    }
}
