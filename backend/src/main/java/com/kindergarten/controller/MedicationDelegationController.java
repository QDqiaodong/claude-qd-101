package com.kindergarten.controller;

import com.kindergarten.dto.MedicationActionRequest;
import com.kindergarten.entity.MedicationDelegation;
import com.kindergarten.service.MedicationDelegationService;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/medication-delegations")
public class MedicationDelegationController {

    private final MedicationDelegationService service;

    public MedicationDelegationController(MedicationDelegationService service) {
        this.service = service;
    }

    @GetMapping
    public List<MedicationDelegation> list(@RequestParam(required = false) Long classroomId,
                                           @RequestParam(required = false) String status,
                                           @RequestParam(required = false) String keyword) {
        return service.list(classroomId, status, keyword);
    }

    @PostMapping
    public MedicationDelegation create(@RequestBody MedicationDelegation input) {
        return service.create(input);
    }

    @PostMapping("/{id}/execute")
    public MedicationDelegation execute(@PathVariable Long id,
                                        @RequestBody(required = false) MedicationActionRequest action) {
        return service.execute(id, action);
    }

    @PostMapping("/{id}/return")
    public MedicationDelegation returnByParent(@PathVariable Long id,
                                               @RequestBody(required = false) MedicationActionRequest action) {
        return service.returnByParent(id, action);
    }

    @PostMapping("/{id}/close")
    public MedicationDelegation close(@PathVariable Long id,
                                      @RequestBody(required = false) MedicationActionRequest action) {
        return service.closeWithoutMedicine(id, action);
    }
}
